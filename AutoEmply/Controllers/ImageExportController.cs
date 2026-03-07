using AutoEmply.Models;
using AutoEmply.Services;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

/// <summary>
/// 이미지/PDF 업로드 → AI 분석 → JSON 또는 ZIP을 반환하는 엔드포인트 모음.
/// 내부적으로 ImageGenerationService에 모든 처리를 위임한다.
/// </summary>
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
        CancellationToken ct)
    {
        _ = useV2;
        var result = await imageGenerationService.GenerateLayoutSpecAsync(formName, image, presetId, ct);
        return result.Success ? Ok(result.Value) : ToErrorResponse(result);
    }

    [HttpPost("export-from-image")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> ExportFromImage(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        [FromForm] bool useV2,
        CancellationToken ct)
    {
        _ = useV2;
        var result = await imageGenerationService.ExportZipAsync(formName, image, presetId, ct);
        return result.Success
            ? File(result.Value!.Bytes, "application/zip", result.Value.FileName)
            : ToErrorResponse(result);
    }

    [HttpPost("documents/{docId}/generate")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateDocumentLayout(
        [FromRoute] string docId,
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        [FromForm] bool useV2,
        CancellationToken ct)
    {
        _ = docId;
        _ = useV2;
        var result = await imageGenerationService.GenerateLayoutSpecAsync(formName, image, presetId, ct);
        return result.Success ? Ok(result.Value) : ToErrorResponse(result);
    }

    [HttpPost("generate-json-v2")]
    [Consumes("multipart/form-data")]
    public Task<IActionResult> GenerateJsonV2(
        [FromForm] string formName, [FromForm] IFormFile image,
        [FromForm] Guid? presetId, CancellationToken ct)
        => GenerateJson(formName, image, presetId, useV2: false, ct);

    [HttpPost("export-from-image-v2")]
    [Consumes("multipart/form-data")]
    public Task<IActionResult> ExportFromImageV2(
        [FromForm] string formName, [FromForm] IFormFile image,
        [FromForm] Guid? presetId, CancellationToken ct)
        => ExportFromImage(formName, image, presetId, useV2: false, ct);

    [HttpPost("generate-structure")]
    [Consumes("multipart/form-data")]
    public async Task<IActionResult> GenerateStructure(
        [FromForm] string formName,
        [FromForm] IFormFile image,
        [FromForm] Guid? presetId,
        CancellationToken ct)
    {
        var result = await imageGenerationService.GenerateStructureAsync(formName, image, presetId, ct);
        return result.Success ? Ok(result.Value) : ToErrorResponse(result);
    }

    /// <summary>ServiceResult 실패를 적절한 HTTP 응답으로 변환.</summary>
    private IActionResult ToErrorResponse<T>(ServiceResult<T> result) => result.StatusCode switch
    {
        404 => NotFound(new { error = result.Error }),
        400 => BadRequest(new { error = result.Error }),
        _ => StatusCode(result.StatusCode, new { error = result.Error, details = result.Details })
    };
}
