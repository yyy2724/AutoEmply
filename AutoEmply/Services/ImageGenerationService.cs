using AutoEmply.Models;
using AutoEmply.Services.Prompts;

namespace AutoEmply.Services;

public sealed class ImageGenerationService(
    ClaudeClient claudeClient,
    DelphiGenerator delphiGenerator,
    LayoutPostProcessor layoutPostProcessor,
    PromptPresetService promptPresetService,
    IConfiguration configuration)
{
    private const long MaxImageBytes = 5 * 1024 * 1024;

    public async Task<ServiceResult<LayoutSpec>> GenerateLayoutSpecAsync(
        string formName,
        IFormFile image,
        Guid? presetId,
        CancellationToken cancellationToken)
    {
        var trimmedName = formName.Trim();
        var preset = await promptPresetService.ResolveAsync(presetId, cancellationToken);
        if (preset is null)
        {
            return ServiceResult<LayoutSpec>.Fail(404, "Prompt preset not found.");
        }

        var imageCheck = await ValidateAndReadImageAsync(image, cancellationToken);
        if (!imageCheck.Success)
        {
            return ServiceResult<LayoutSpec>.Fail(400, imageCheck.Error!);
        }

        var result = await GenerateLayoutSpecWithServerTimeoutAsync(
            trimmedName,
            imageCheck.MediaType!,
            imageCheck.Base64Data!,
            preset);

        if (!result.Success)
        {
            return ServiceResult<LayoutSpec>.Fail(result.StatusCode, result.Error ?? "Layout generation failed.", result.Details);
        }

        var layoutSpec = layoutPostProcessor.Process(result.LayoutSpec!);
        return ServiceResult<LayoutSpec>.Ok(layoutSpec);
    }

    public async Task<ServiceResult<ExportArtifact>> ExportZipAsync(
        string formName,
        IFormFile image,
        Guid? presetId,
        CancellationToken cancellationToken)
    {
        var trimmedName = formName.Trim();
        var layoutResult = await GenerateLayoutSpecAsync(trimmedName, image, presetId, cancellationToken);
        if (!layoutResult.Success)
        {
            return ServiceResult<ExportArtifact>.Fail(layoutResult.StatusCode, layoutResult.Error!, layoutResult.Details);
        }

        var bytes = delphiGenerator.GenerateZip(trimmedName, layoutResult.Value!);
        return ServiceResult<ExportArtifact>.Ok(new ExportArtifact(bytes, $"{trimmedName}.zip"));
    }

    public async Task<ServiceResult<FormStructure>> GenerateStructureAsync(
        string formName,
        IFormFile image,
        Guid? presetId,
        CancellationToken cancellationToken)
    {
        var trimmedName = formName.Trim();
        var preset = await promptPresetService.ResolveAsync(presetId, cancellationToken);
        if (preset is null)
        {
            return ServiceResult<FormStructure>.Fail(404, "Prompt preset not found.");
        }

        var imageCheck = await ValidateAndReadImageAsync(image, cancellationToken);
        if (!imageCheck.Success)
        {
            return ServiceResult<FormStructure>.Fail(400, imageCheck.Error!);
        }

        var result = await GenerateFormStructureWithServerTimeoutAsync(
            trimmedName,
            imageCheck.MediaType!,
            imageCheck.Base64Data!,
            preset);

        if (!result.Success)
        {
            return ServiceResult<FormStructure>.Fail(result.StatusCode, result.Error ?? "Structure generation failed.", result.Details);
        }

        return ServiceResult<FormStructure>.Ok(result.FormStructure!);
    }

    private async Task<ClaudeLayoutResult> GenerateLayoutSpecWithServerTimeoutAsync(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset)
    {
        var timeoutSeconds = configuration.GetValue<int?>("Anthropic:RequestTimeoutSeconds") ?? 240;
        using var aiCts = new CancellationTokenSource(TimeSpan.FromSeconds(Math.Max(30, timeoutSeconds)));

        return await claudeClient.GenerateLayoutSpecAsync(
            formName,
            mediaType,
            fileBase64,
            preset,
            aiCts.Token);
    }

    private async Task<ClaudeFormStructureResult> GenerateFormStructureWithServerTimeoutAsync(
        string formName,
        string mediaType,
        string fileBase64,
        ResolvedPromptPreset preset)
    {
        var timeoutSeconds = configuration.GetValue<int?>("Anthropic:RequestTimeoutSeconds") ?? 240;
        using var aiCts = new CancellationTokenSource(TimeSpan.FromSeconds(Math.Max(30, timeoutSeconds)));

        return await claudeClient.GenerateFormStructureAsync(
            formName,
            mediaType,
            fileBase64,
            preset,
            aiCts.Token);
    }

    private static async Task<ImageValidationResult> ValidateAndReadImageAsync(IFormFile? image, CancellationToken cancellationToken)
    {
        if (image is null || image.Length == 0)
        {
            return ImageValidationResult.Fail("Image/PDF file is required.");
        }

        if (image.Length > MaxImageBytes)
        {
            return ImageValidationResult.Fail("Image/PDF size exceeded (max 5MB).");
        }

        var mediaType = ResolveMediaType(image.FileName, image.ContentType);
        if (mediaType is null)
        {
            return ImageValidationResult.Fail("Unsupported file type.");
        }

        await using var stream = image.OpenReadStream();
        using var memory = new MemoryStream();
        await stream.CopyToAsync(memory, cancellationToken);
        var base64 = Convert.ToBase64String(memory.ToArray());
        return ImageValidationResult.Ok(mediaType, base64);
    }

    private static string? ResolveMediaType(string fileName, string? contentType)
    {
        var byExt = GetMediaTypeFromExtension(Path.GetExtension(fileName));
        var byContentType = NormalizeContentType(contentType);

        if (byExt is not null && byContentType is not null && !string.Equals(byExt, byContentType, StringComparison.Ordinal))
        {
            return null;
        }

        return byExt ?? byContentType;
    }

    private static string? NormalizeContentType(string? contentType)
    {
        if (string.IsNullOrWhiteSpace(contentType))
        {
            return null;
        }

        return contentType.Trim().ToLowerInvariant() switch
        {
            "image/jpg" => "image/jpeg",
            "image/jpeg" => "image/jpeg",
            "image/png" => "image/png",
            "image/gif" => "image/gif",
            "image/webp" => "image/webp",
            "application/pdf" => "application/pdf",
            _ => null
        };
    }

    private static string? GetMediaTypeFromExtension(string extension)
    {
        return extension.Trim().ToLowerInvariant() switch
        {
            ".jpg" => "image/jpeg",
            ".jpeg" => "image/jpeg",
            ".png" => "image/png",
            ".gif" => "image/gif",
            ".webp" => "image/webp",
            ".pdf" => "application/pdf",
            _ => null
        };
    }
}

public sealed record ServiceResult<T>(
    bool Success,
    int StatusCode,
    string? Error,
    IReadOnlyCollection<string>? Details,
    T? Value)
{
    public static ServiceResult<T> Ok(T value) =>
        new(true, 200, null, null, value);

    public static ServiceResult<T> Fail(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
        new(false, statusCode, error, details, default);
}

public sealed record ExportArtifact(byte[] Bytes, string FileName);

internal sealed record ImageValidationResult(bool Success, string? Error, string? MediaType, string? Base64Data)
{
    public static ImageValidationResult Ok(string mediaType, string base64Data) =>
        new(true, null, mediaType, base64Data);

    public static ImageValidationResult Fail(string error) =>
        new(false, error, null, null);
}
