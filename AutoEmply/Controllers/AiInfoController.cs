using AutoEmply.Services;
using Microsoft.AspNetCore.Mvc;

namespace AutoEmply.Controllers;

/// <summary>
/// 현재 사용 중인 AI 모델 정보를 반환하는 엔드포인트.
/// 클라이언트 UI 하단에 "AI 버전" 표시용.
/// </summary> 아이고..
[ApiController]
[Route("api")]
public sealed class AiInfoController(IConfiguration configuration, AiModelState aiModelState) : ControllerBase
{
    [HttpGet("ai-version")]
    public IActionResult GetAiVersion()
    {
        var configuredModel = configuration["Anthropic:Model"] ?? "unknown";
        var runtimeModel = aiModelState.LastResponseModel ?? configuredModel;

        return Ok(new
        {
            version = runtimeModel,
            model = runtimeModel,
            configuredModel,
            source = aiModelState.LastResponseModel is null ? "configured" : "runtime"
        });
    }
}
