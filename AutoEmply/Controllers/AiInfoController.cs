using Microsoft.AspNetCore.Mvc;
using AutoEmply.Services;

namespace AutoEmply.Controllers;

[ApiController]
[Route("api")]
public sealed class AiInfoController(
    IConfiguration configuration,
    AiModelState aiModelState) : ControllerBase
{
    [HttpGet("ai-version")]
    public IActionResult GetAiVersion()
    {
        var configuredModel = configuration["Anthropic:Model"] ?? "unknown";
        var model = aiModelState.LastResponseModel ?? configuredModel;
        return Ok(new
        {
            version = model,
            model,
            configuredModel,
            source = aiModelState.LastResponseModel is null ? "configured" : "runtime"
        });
    }
}
