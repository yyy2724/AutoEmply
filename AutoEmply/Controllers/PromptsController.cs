using AutoEmply.Dtos;
using AutoEmply.Services.Prompts;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

/// <summary>
/// 프롬프트 프리셋 CRUD API.
/// 프리셋 = Claude에게 보낼 시스템 프롬프트 + 사용자 프롬프트 템플릿 + AI 파라미터 묶음.
/// </summary>
[ApiController]
[Route("api/prompts")]
public sealed class PromptsController(PromptPresetService presetService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetAll(CancellationToken ct) =>
        Ok(await presetService.GetAllAsync(ct));

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreatePromptPresetRequest request, CancellationToken ct)
    {
        try { return Ok(await presetService.CreateAsync(request, ct)); }
        catch (ArgumentException ex) { return BadRequest(new { error = ex.Message }); }
        catch (DbUpdateException ex) { return Conflict(new { error = $"Failed to create preset: {ex.InnerException?.Message ?? ex.Message}" }); }
    }

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdatePromptPresetRequest request, CancellationToken ct)
    {
        try
        {
            var updated = await presetService.UpdateAsync(id, request, ct);
            return updated is null ? NotFound(new { error = "Preset not found." }) : Ok(updated);
        }
        catch (ArgumentException ex) { return BadRequest(new { error = ex.Message }); }
        catch (DbUpdateException ex) { return Conflict(new { error = $"Failed to update preset: {ex.InnerException?.Message ?? ex.Message}" }); }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id, CancellationToken ct)
    {
        try
        {
            var deleted = await presetService.DeleteAsync(id, ct);
            return deleted ? NoContent() : NotFound(new { error = "Preset not found." });
        }
        catch (DbUpdateException ex) { return Conflict(new { error = $"Failed to delete preset: {ex.InnerException?.Message ?? ex.Message}" }); }
    }
}
