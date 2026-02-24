using AutoEmply.Models;
using AutoEmply.Services;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

[ApiController]
[Route("api/[controller]")]
public sealed class ExportController(DelphiGenerator generator) : ControllerBase
{
    [HttpPost]
    public IActionResult Export([FromBody] ExportRequest request)
    {
        var errors = LayoutSpecValidator.Validate(request.FormName.Trim(), request.LayoutSpec);
        if (errors.Count > 0)
        {
            return BadRequest(new
            {
                error = "Invalid request",
                details = errors
            });
        }

        var bytes = generator.GenerateZip(request.FormName.Trim(), request.LayoutSpec!);
        return File(bytes, "application/zip", $"{request.FormName.Trim()}.zip");
    }
}
