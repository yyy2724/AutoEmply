namespace AutoEmply.Services.Prompts;

public sealed record ResolvedPromptPreset(
    Guid Id,
    string Name,
    string SystemPrompt,
    string UserPromptTemplate,
    string? StyleRulesJson,
    string Model,
    decimal Temperature,
    int MaxTokens);
