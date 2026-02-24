namespace AutoEmply.Dtos;

public sealed record PromptPresetDto(
    Guid Id,
    string Name,
    string SystemPrompt,
    string UserPromptTemplate,
    string? StyleRulesJson,
    string? Model,
    decimal? Temperature,
    int? MaxTokens,
    bool IsActive,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);

public sealed record CreatePromptPresetRequest(
    string Name,
    string SystemPrompt,
    string UserPromptTemplate,
    string? StyleRulesJson,
    string? Model,
    decimal? Temperature,
    int? MaxTokens,
    bool IsActive = true);

public sealed record UpdatePromptPresetRequest(
    string Name,
    string SystemPrompt,
    string UserPromptTemplate,
    string? StyleRulesJson,
    string? Model,
    decimal? Temperature,
    int? MaxTokens,
    bool IsActive);
