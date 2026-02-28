using AutoEmply.Dtos;
using AutoEmply.Services.Prompts;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

[ApiController]
[Route("api/prompts")]
public sealed class PromptsController(PromptPresetService promptPresetService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> GetAll(CancellationToken cancellationToken)
    {
        var presets = await promptPresetService.GetAllAsync(cancellationToken);
        return Ok(presets);
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreatePromptPresetRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var created = await promptPresetService.CreateAsync(request, cancellationToken);
            return Ok(created);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(new { error = ex.Message });
        }
        catch (DbUpdateException ex)
        {
            return Conflict(new { error = $"Failed to create preset: {ex.InnerException?.Message ?? ex.Message}" });
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdatePromptPresetRequest request, CancellationToken cancellationToken)
    {
        try
        {
            var updated = await promptPresetService.UpdateAsync(id, request, cancellationToken);
            if (updated is null)
            {
                return NotFound(new { error = "Preset not found." });
            }

            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(new { error = ex.Message });
        }
        catch (DbUpdateException ex)
        {
            return Conflict(new { error = $"Failed to update preset: {ex.InnerException?.Message ?? ex.Message}" });
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id, CancellationToken cancellationToken)
    {
        try
        {
            var deleted = await promptPresetService.DeleteAsync(id, cancellationToken);
            if (!deleted)
            {
                return NotFound(new { error = "Preset not found." });
            }

            return NoContent();
        }
        catch (DbUpdateException ex)
        {
            return Conflict(new { error = $"Failed to delete preset: {ex.InnerException?.Message ?? ex.Message}" });
        }
    }
}
