using AutoEmply.Models;

namespace AutoEmply.Services;

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
