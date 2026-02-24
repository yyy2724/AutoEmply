namespace AutoEmply.Entities;

public sealed class PromptPreset
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string SystemPrompt { get; set; } = string.Empty;
    public string UserPromptTemplate { get; set; } = string.Empty;
    public string? StyleRulesJson { get; set; }
    public string? Model { get; set; }
    public decimal? Temperature { get; set; }
    public int? MaxTokens { get; set; }
    public bool IsActive { get; set; } = true;
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }

    public ICollection<PromptVersion> Versions { get; set; } = new List<PromptVersion>();
}
