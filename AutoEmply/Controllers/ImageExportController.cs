using AutoEmply.Services;
using AutoEmply.Services.Prompts;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

[ApiController]
[Route("api")]
public sealed class ImageExportController(
    ClaudeClient claudeClient,
    DelphiGenerator delphiGenerator,
    PromptPresetService promptPresetService) : ControllerBase
{
    private const long MaxImageBytes = 5 * 1024 * 1024;

    [HttpPost("generate-json")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateJson(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken cancellationToken)
    {
        var preset = await promptPresetService.ResolveAsync(presetId, cancellationToken);
        if (preset is null)
        {
            return NotFound(new { error = "Prompt preset not found." });
        }

        var imageCheck = await ValidateAndReadImageAsync(image, cancellationToken);
        if (!imageCheck.Success)
        {
            return BadRequest(new { error = imageCheck.Error });
        }

        var result = await claudeClient.GenerateLayoutSpecAsync(
            formName.Trim(),
            imageCheck.MediaType!,
            imageCheck.Base64Data!,
            preset,
            cancellationToken);

        if (!result.Success)
        {
            return StatusCode(result.StatusCode, new
            {
                error = result.Error,
                details = result.Details
            });
        }

        return Ok(result.LayoutSpec);
    }

    [HttpPost("export-from-image")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> ExportFromImage(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken cancellationToken)
    {
        var preset = await promptPresetService.ResolveAsync(presetId, cancellationToken);
        if (preset is null)
        {
            return NotFound(new { error = "Prompt preset not found." });
        }

        var imageCheck = await ValidateAndReadImageAsync(image, cancellationToken);
        if (!imageCheck.Success)
        {
            return BadRequest(new { error = imageCheck.Error });
        }

        var trimmedName = formName.Trim();
        var result = await claudeClient.GenerateLayoutSpecAsync(
            trimmedName,
            imageCheck.MediaType!,
            imageCheck.Base64Data!,
            preset,
            cancellationToken);

        if (!result.Success)
        {
            return StatusCode(result.StatusCode, new
            {
                error = result.Error,
                details = result.Details
            });
        }

        var bytes = delphiGenerator.GenerateZip(trimmedName, result.LayoutSpec!);
        return File(bytes, "application/zip", $"{trimmedName}.zip");
    }

    [HttpPost("documents/{docId}/generate")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateDocumentLayout(
        [FromRoute] string docId,
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken cancellationToken)
    {
        _ = docId;

        var preset = await promptPresetService.ResolveAsync(presetId, cancellationToken);
        if (preset is null)
        {
            return NotFound(new { error = "Prompt preset not found." });
        }

        var imageCheck = await ValidateAndReadImageAsync(image, cancellationToken);
        if (!imageCheck.Success)
        {
            return BadRequest(new { error = imageCheck.Error });
        }

        var result = await claudeClient.GenerateLayoutSpecAsync(
            formName.Trim(),
            imageCheck.MediaType!,
            imageCheck.Base64Data!,
            preset,
            cancellationToken);

        if (!result.Success)
        {
            return StatusCode(result.StatusCode, new
            {
                error = result.Error,
                details = result.Details
            });
        }

        return Ok(result.LayoutSpec);
    }

    private static async Task<ImageValidationResult> ValidateAndReadImageAsync(IFormFile? image, CancellationToken cancellationToken)
    {
        if (image is null || image.Length == 0)
        {
            return ImageValidationResult.Fail("\uC774\uBBF8\uC9C0/PDF \uD30C\uC77C\uC774 \uD544\uC694\uD569\uB2C8\uB2E4.");
        }

        if (image.Length > MaxImageBytes)
        {
            return ImageValidationResult.Fail("\uC774\uBBF8\uC9C0/PDF \uC6A9\uB7C9 \uCD08\uACFC(\uCD5C\uB300 5MB)");
        }

        var mediaType = ResolveMediaType(image.FileName, image.ContentType);
        if (mediaType is null)
        {
            return ImageValidationResult.Fail("\uC9C0\uC6D0\uD558\uC9C0 \uC54A\uB294 \uD30C\uC77C \uD615\uC2DD\uC785\uB2C8\uB2E4.");
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

public sealed record ImageValidationResult(bool Success, string? Error, string? MediaType, string? Base64Data)
{
    public static ImageValidationResult Ok(string mediaType, string base64Data) =>
        new(true, null, mediaType, base64Data);

    public static ImageValidationResult Fail(string error) =>
        new(false, error, null, null);
}
