namespace AutoEmply.Entities;

public sealed class PromptVersion
{
    public Guid Id { get; set; }
    public Guid PresetId { get; set; }
    public int Version { get; set; }
    public string SystemPrompt { get; set; } = string.Empty;
    public string UserPromptTemplate { get; set; } = string.Empty;
    public string? StyleRulesJson { get; set; }
    public DateTimeOffset CreatedAt { get; set; }

    public PromptPreset Preset { get; set; } = null!;
}
