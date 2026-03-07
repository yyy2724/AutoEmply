using AutoEmply.Models;
using AutoEmply.Services.Prompts;

namespace AutoEmply.Services;

/// <summary>
/// 이미지/PDF 업로드 → Claude AI 분석 → LayoutSpec 또는 ZIP 생성까지의 전체 흐름을 조율하는 서비스.
/// Controller가 직접 ClaudeClient를 호출하지 않고, 이 서비스를 통해 일관된 파이프라인을 탄다.
///
/// [이미지 업로드] → ValidateAndRead → [Claude AI] → PostProcess → [LayoutSpec / ZIP]
/// </summary>
public sealed class ImageGenerationService(
    ClaudeClient claudeClient,
    DelphiGenerator delphiGenerator,
    LayoutPostProcessor layoutPostProcessor,
    PromptPresetService promptPresetService,
    IConfiguration configuration)
{
    private const long MaxImageBytes = 5 * 1024 * 1024;

    // ───────────────────────────────────────────
    //  공개 API
    // ───────────────────────────────────────────

    /// <summary>이미지 → Claude → LayoutSpec JSON 반환.</summary>
    public async Task<ServiceResult<LayoutSpec>> GenerateLayoutSpecAsync(
        string formName, IFormFile image, Guid? presetId, CancellationToken ct)
    {
        var (preset, imageData, earlyFail) = await PrepareAsync(formName, image, presetId, ct);
        if (earlyFail is not null)
            return ServiceResult<LayoutSpec>.Fail(earlyFail.Value.Code, earlyFail.Value.Message);

        var result = await CallClaudeWithTimeoutAsync(
            (token) => claudeClient.GenerateLayoutSpecAsync(
                formName.Trim(), imageData!.MediaType, imageData.Base64Data, preset!, token));

        if (!result.Success)
            return ServiceResult<LayoutSpec>.Fail(result.StatusCode, result.Error ?? "Layout generation failed.", result.Details);

        var processed = layoutPostProcessor.Process(result.LayoutSpec!);
        return ServiceResult<LayoutSpec>.Ok(processed);
    }

    /// <summary>이미지 → Claude → Delphi ZIP(dfm+pas) 반환.</summary>
    public async Task<ServiceResult<ExportArtifact>> ExportZipAsync(
        string formName, IFormFile image, Guid? presetId, CancellationToken ct)
    {
        var trimmedName = formName.Trim();
        var layoutResult = await GenerateLayoutSpecAsync(trimmedName, image, presetId, ct);
        if (!layoutResult.Success)
            return ServiceResult<ExportArtifact>.Fail(layoutResult.StatusCode, layoutResult.Error!, layoutResult.Details);

        var bytes = delphiGenerator.GenerateZip(trimmedName, layoutResult.Value!);
        return ServiceResult<ExportArtifact>.Ok(new ExportArtifact(bytes, $"{trimmedName}.zip"));
    }

    /// <summary>이미지 → Claude → FormStructure(논리 구조) 반환.</summary>
    public async Task<ServiceResult<FormStructure>> GenerateStructureAsync(
        string formName, IFormFile image, Guid? presetId, CancellationToken ct)
    {
        var (preset, imageData, earlyFail) = await PrepareAsync(formName, image, presetId, ct);
        if (earlyFail is not null)
            return ServiceResult<FormStructure>.Fail(earlyFail.Value.Code, earlyFail.Value.Message);

        var result = await CallClaudeWithTimeoutAsync(
            (token) => claudeClient.GenerateFormStructureAsync(
                formName.Trim(), imageData!.MediaType, imageData.Base64Data, preset!, token));

        if (!result.Success)
            return ServiceResult<FormStructure>.Fail(result.StatusCode, result.Error ?? "Structure generation failed.", result.Details);

        return ServiceResult<FormStructure>.Ok(result.FormStructure!);
    }

    // ───────────────────────────────────────────
    //  내부 헬퍼
    // ───────────────────────────────────────────

    /// <summary>프리셋 조회 + 이미지 검증을 한 번에 수행.</summary>
    private async Task<(ResolvedPromptPreset? Preset, ImageData? Image, (int Code, string Message)? Failure)>
        PrepareAsync(string formName, IFormFile image, Guid? presetId, CancellationToken ct)
    {
        var preset = await promptPresetService.ResolveAsync(presetId, ct);
        if (preset is null)
            return (null, null, (404, "Prompt preset not found."));

        var imageData = await ValidateAndReadImageAsync(image, ct);
        if (imageData is null)
            return (null, null, (400, "Image validation failed."));

        return (preset, imageData, null);
    }

    /// <summary>설정된 타임아웃 내에서 Claude API를 호출한다.</summary>
    private async Task<T> CallClaudeWithTimeoutAsync<T>(Func<CancellationToken, Task<T>> callClaude)
    {
        var timeoutSeconds = configuration.GetValue<int?>("Anthropic:RequestTimeoutSeconds") ?? 240;
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(Math.Max(30, timeoutSeconds)));
        return await callClaude(cts.Token);
    }

    // ───────────────────────────────────────────
    //  이미지 검증
    // ───────────────────────────────────────────

    private static async Task<ImageData?> ValidateAndReadImageAsync(IFormFile? image, CancellationToken ct)
    {
        if (image is null || image.Length == 0) return null;
        if (image.Length > MaxImageBytes) return null;

        var mediaType = ResolveMediaType(image.FileName, image.ContentType);
        if (mediaType is null) return null;

        await using var stream = image.OpenReadStream();
        using var memory = new MemoryStream();
        await stream.CopyToAsync(memory, ct);
        return new ImageData(mediaType, Convert.ToBase64String(memory.ToArray()));
    }

    private static string? ResolveMediaType(string fileName, string? contentType)
    {
        var byExt = MapExtensionToMediaType(Path.GetExtension(fileName));
        var byCt = NormalizeContentType(contentType);

        // 확장자와 Content-Type이 모두 있는데 불일치하면 거부
        if (byExt is not null && byCt is not null && byExt != byCt)
            return null;

        return byExt ?? byCt;
    }

    private static string? NormalizeContentType(string? contentType) =>
        contentType?.Trim().ToLowerInvariant() switch
        {
            "image/jpg" or "image/jpeg" => "image/jpeg",
            "image/png" => "image/png",
            "image/gif" => "image/gif",
            "image/webp" => "image/webp",
            "application/pdf" => "application/pdf",
            _ => null
        };

    private static string? MapExtensionToMediaType(string extension) =>
        extension.Trim().ToLowerInvariant() switch
        {
            ".jpg" or ".jpeg" => "image/jpeg",
            ".png" => "image/png",
            ".gif" => "image/gif",
            ".webp" => "image/webp",
            ".pdf" => "application/pdf",
            _ => null
        };

    /// <summary>검증을 통과한 이미지 데이터.</summary>
    private sealed record ImageData(string MediaType, string Base64Data);
}
