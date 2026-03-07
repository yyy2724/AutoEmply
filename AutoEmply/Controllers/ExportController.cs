using AutoEmply.Models;
using AutoEmply.Services;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

/// <summary>
/// 클라이언트가 직접 편집한 LayoutSpec JSON을 받아 Delphi ZIP을 반환하는 엔드포인트.
/// (AI 생성 없이, 이미 만들어진 JSON을 내보내기만 할 때 사용)
/// </summary>
[ApiController]
[Route("api/[controller]")]
public sealed class ExportController(DelphiGenerator generator) : ControllerBase
{
    [HttpPost]
    public IActionResult Export([FromBody] ExportRequest request)
    {
        var formName = request.FormName.Trim();
        var errors = LayoutSpecValidator.Validate(formName, request.LayoutSpec);

        if (errors.Count > 0)
            return BadRequest(new { error = "Invalid request", details = errors });

        var bytes = generator.GenerateZip(formName, request.LayoutSpec!);
        return File(bytes, "application/zip", $"{formName}.zip");
    }
}
