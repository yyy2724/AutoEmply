using AutoEmply.Models;

namespace AutoEmply.Services;

/// <summary>
/// Claude AI의 LayoutSpec 생성 결과.
/// ClaudeClient → ImageGenerationService 사이에서 사용된다.
/// </summary>
public sealed record ClaudeLayoutResult(
    bool Success,
    int StatusCode,
    string? Error,
    IReadOnlyCollection<string>? Details,
    LayoutSpec? LayoutSpec)
{
    public static ClaudeLayoutResult Ok(LayoutSpec layoutSpec) =>
        new(true, 200, null, null, layoutSpec);

    public static ClaudeLayoutResult Fail(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
        new(false, statusCode, error, details, null);
}

/// <summary>
/// Claude AI의 FormStructure 추출 결과 (Phase 1).
/// </summary>
public sealed record ClaudeFormStructureResult(
    bool Success,
    int StatusCode,
    string? Error,
    IReadOnlyCollection<string>? Details,
    FormStructure? FormStructure)
{
    public static ClaudeFormStructureResult Ok(FormStructure structure) =>
        new(true, 200, null, null, structure);

    public static ClaudeFormStructureResult Fail(int statusCode, string error, IReadOnlyCollection<string>? details = null) =>
        new(false, statusCode, error, details, null);
}
