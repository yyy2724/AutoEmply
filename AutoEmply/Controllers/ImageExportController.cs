using AutoEmply.Services;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

[ApiController]
[Route("api")]
public sealed class ImageExportController(ImageGenerationService imageGenerationService) : ControllerBase
{
    [HttpPost("generate-json")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateJson(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        [FromForm] bool useV2,
        CancellationToken cancellationToken)
    {
        _ = useV2;
        var result = await imageGenerationService.GenerateLayoutSpecAsync(formName, image, presetId, cancellationToken);
        if (!result.Success) return Failure(result);
        return Ok(result.Value);
    }

    [HttpPost("export-from-image")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> ExportFromImage(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        [FromForm] bool useV2,
        CancellationToken cancellationToken)
    {
        _ = useV2;
        var result = await imageGenerationService.ExportZipAsync(formName, image, presetId, cancellationToken);
        if (!result.Success) return Failure(result);
        return File(result.Value!.Bytes, "application/zip", result.Value.FileName);
    }

    [HttpPost("documents/{docId}/generate")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateDocumentLayout(
        [FromRoute] string docId,
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        [FromForm] bool useV2,
        CancellationToken cancellationToken)
    {
        _ = docId;
        _ = useV2;

        var result = await imageGenerationService.GenerateLayoutSpecAsync(formName, image, presetId, cancellationToken);
        if (!result.Success) return Failure(result);
        return Ok(result.Value);
    }

    [HttpPost("generate-json-v2")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateJsonV2(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken cancellationToken)
    {
        return await GenerateJson(formName, image, presetId, useV2: false, cancellationToken);
    }

    [HttpPost("export-from-image-v2")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> ExportFromImageV2(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken cancellationToken)
    {
        return await ExportFromImage(formName, image, presetId, useV2: false, cancellationToken);
    }

    [HttpPost("generate-structure")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateStructure(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken cancellationToken)
    {
        var result = await imageGenerationService.GenerateStructureAsync(formName, image, presetId, cancellationToken);
        if (!result.Success) return Failure(result);
        return Ok(result.Value);
    }

    private IActionResult Failure<T>(ServiceResult<T> result)
    {
        if (result.StatusCode == 404)
        {
            return NotFound(new { error = result.Error });
        }

        if (result.StatusCode == 400)
        {
            return BadRequest(new { error = result.Error });
        }

        return StatusCode(result.StatusCode, new
        {
            error = result.Error,
            details = result.Details
        });
    }
}
