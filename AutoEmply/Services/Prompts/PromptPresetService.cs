using AutoEmply.Data;
using AutoEmply.Dtos;
using AutoEmply.Entities;
using Microsoft.EntityFrameworkCore;

namespace AutoEmply.Services.Prompts;

public sealed class PromptPresetService(AppDbContext dbContext, IConfiguration configuration)
{
    private const string DefaultModel = "claude-sonnet-4-6";
    private const decimal DefaultTemperature = 0m;
    private const int DefaultMaxTokens = 32000;

    public async Task<IReadOnlyList<PromptPresetDto>> GetAllAsync(CancellationToken cancellationToken)
    {
        return await dbContext.PromptPresets
            .AsNoTracking()
            .OrderByDescending(x => x.IsActive)
            .ThenBy(x => x.Name)
            .Select(ToDtoExpression())
            .ToListAsync(cancellationToken);
    }

    public async Task<PromptPresetDto> CreateAsync(CreatePromptPresetRequest request, CancellationToken cancellationToken)
    {
        ValidateRequiredFields(request.Name, request.SystemPrompt, request.UserPromptTemplate);
        ValidateJsonIfProvided(request.StyleRulesJson);

        var now = DateTimeOffset.UtcNow;
        var entity = new PromptPreset
        {
            Id = Guid.NewGuid(),
            Name = request.Name.Trim(),
            SystemPrompt = request.SystemPrompt.Trim(),
            UserPromptTemplate = request.UserPromptTemplate.Trim(),
            StyleRulesJson = NormalizeNullable(request.StyleRulesJson),
            Model = NormalizeNullable(request.Model),
            Temperature = request.Temperature,
            MaxTokens = request.MaxTokens,
            IsActive = request.IsActive,
            CreatedAt = now,
            UpdatedAt = now
        };

        dbContext.PromptPresets.Add(entity);
        dbContext.PromptVersions.Add(new PromptVersion
        {
            Id = Guid.NewGuid(),
            PresetId = entity.Id,
            Version = 1,
            SystemPrompt = entity.SystemPrompt,
            UserPromptTemplate = entity.UserPromptTemplate,
            StyleRulesJson = entity.StyleRulesJson,
            CreatedAt = now
        });

        await dbContext.SaveChangesAsync(cancellationToken);
        return ToDto(entity);
    }

    public async Task<PromptPresetDto?> UpdateAsync(Guid id, UpdatePromptPresetRequest request, CancellationToken cancellationToken)
    {
        ValidateRequiredFields(request.Name, request.SystemPrompt, request.UserPromptTemplate);
        ValidateJsonIfProvided(request.StyleRulesJson);

        var entity = await dbContext.PromptPresets
            .Include(x => x.Versions)
            .FirstOrDefaultAsync(x => x.Id == id, cancellationToken);

        if (entity is null)
        {
            return null;
        }

        entity.Name = request.Name.Trim();
        entity.SystemPrompt = request.SystemPrompt.Trim();
        entity.UserPromptTemplate = request.UserPromptTemplate.Trim();
        entity.StyleRulesJson = NormalizeNullable(request.StyleRulesJson);
        entity.Model = NormalizeNullable(request.Model);
        entity.Temperature = request.Temperature;
        entity.MaxTokens = request.MaxTokens;
        entity.IsActive = request.IsActive;
        entity.UpdatedAt = DateTimeOffset.UtcNow;

        var nextVersion = entity.Versions.Count == 0 ? 1 : entity.Versions.Max(x => x.Version) + 1;
        dbContext.PromptVersions.Add(new PromptVersion
        {
            Id = Guid.NewGuid(),
            PresetId = entity.Id,
            Version = nextVersion,
            SystemPrompt = entity.SystemPrompt,
            UserPromptTemplate = entity.UserPromptTemplate,
            StyleRulesJson = entity.StyleRulesJson,
            CreatedAt = DateTimeOffset.UtcNow
        });

        await dbContext.SaveChangesAsync(cancellationToken);
        return ToDto(entity);
    }

    public async Task<bool> DeleteAsync(Guid id, CancellationToken cancellationToken)
    {
        var entity = await dbContext.PromptPresets
            .FirstOrDefaultAsync(x => x.Id == id, cancellationToken);

        if (entity is null)
        {
            return false;
        }

        dbContext.PromptPresets.Remove(entity);
        await dbContext.SaveChangesAsync(cancellationToken);
        return true;
    }

    public async Task<ResolvedPromptPreset?> ResolveAsync(Guid? presetId, CancellationToken cancellationToken)
    {
        if (presetId.HasValue)
        {
            var specificPreset = await dbContext.PromptPresets
                .AsNoTracking()
                .FirstOrDefaultAsync(x => x.Id == presetId.Value, cancellationToken);

            if (specificPreset is null)
            {
                return null;
            }

            var otherActivePresets = await dbContext.PromptPresets
                .AsNoTracking()
                .Where(x => x.IsActive && x.Id != specificPreset.Id)
                .OrderByDescending(x => x.UpdatedAt)
                .ToListAsync(cancellationToken);

            var specificMergedSystemPrompt = BuildPromptWithReferences(
                specificPreset.SystemPrompt,
                otherActivePresets.Select(x => x.SystemPrompt),
                "PastPromptReference");
            var specificMergedUserPromptTemplate = BuildPromptWithReferences(
                specificPreset.UserPromptTemplate,
                otherActivePresets.Select(x => x.UserPromptTemplate),
                "PastUserPromptReference");

            return new ResolvedPromptPreset(
                specificPreset.Id,
                specificPreset.Name,
                specificMergedSystemPrompt,
                specificMergedUserPromptTemplate,
                specificPreset.StyleRulesJson,
                ResolveModel(specificPreset.Model),
                specificPreset.Temperature ?? DefaultTemperature,
                specificPreset.MaxTokens ?? DefaultMaxTokens);
        }

        var activePresets = await dbContext.PromptPresets
            .AsNoTracking()
            .Where(x => x.IsActive)
            .OrderByDescending(x => x.UpdatedAt)
            .ToListAsync(cancellationToken);
        if (activePresets.Count == 0)
        {
            return null;
        }

        var primaryPreset = activePresets[0];
        var secondaryPresets = activePresets.Skip(1).ToList();

        var mergedSystemPrompt = BuildPromptWithReferences(
            primaryPreset.SystemPrompt,
            secondaryPresets.Select(x => x.SystemPrompt),
            "PastPromptReference");
        var mergedUserPromptTemplate = BuildPromptWithReferences(
            primaryPreset.UserPromptTemplate,
            secondaryPresets.Select(x => x.UserPromptTemplate),
            "PastUserPromptReference");

        return new ResolvedPromptPreset(
            primaryPreset.Id,
            primaryPreset.Name,
            mergedSystemPrompt,
            mergedUserPromptTemplate,
            primaryPreset.StyleRulesJson,
            ResolveModel(primaryPreset.Model),
            primaryPreset.Temperature ?? DefaultTemperature,
            primaryPreset.MaxTokens ?? DefaultMaxTokens);
    }

    private static string BuildPromptWithReferences(
        string primaryPrompt,
        IEnumerable<string> secondaryPrompts,
        string sectionTitle)
    {
        var primary = (primaryPrompt ?? string.Empty).Trim();
        var secondary = secondaryPrompts
            .Select(x => (x ?? string.Empty).Trim())
            .Where(x => !string.IsNullOrWhiteSpace(x))
            .Distinct(StringComparer.Ordinal)
            .ToList();

        if (secondary.Count == 0)
        {
            return primary;
        }

        var lines = new List<string> { primary, string.Empty, $"{sectionTitle}:" };
        for (var i = 0; i < secondary.Count; i++)
        {
            lines.Add($"[{i + 1}] {secondary[i]}");
        }

        return string.Join("\n", lines);
    }

    private string ResolveModel(string? presetModel)
    {
        if (!string.IsNullOrWhiteSpace(presetModel))
        {
            return presetModel.Trim();
        }

        return configuration["Anthropic:Model"] ?? DefaultModel;
    }

    private static string? NormalizeNullable(string? value) =>
        string.IsNullOrWhiteSpace(value) ? null : value.Trim();

    private static void ValidateJsonIfProvided(string? rawJson)
    {
        if (string.IsNullOrWhiteSpace(rawJson))
        {
            return;
        }

        try
        {
            _ = System.Text.Json.JsonDocument.Parse(rawJson);
        }
        catch (System.Text.Json.JsonException ex)
        {
            throw new ArgumentException($"styleRulesJson is not valid JSON: {ex.Message}");
        }
    }

    private static void ValidateRequiredFields(string name, string systemPrompt, string userPromptTemplate)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new ArgumentException("name is required.");
        }

        if (string.IsNullOrWhiteSpace(systemPrompt))
        {
            throw new ArgumentException("systemPrompt is required.");
        }

        if (string.IsNullOrWhiteSpace(userPromptTemplate))
        {
            throw new ArgumentException("userPromptTemplate is required.");
        }
    }

    private static System.Linq.Expressions.Expression<Func<PromptPreset, PromptPresetDto>> ToDtoExpression() =>
        x => new PromptPresetDto(
            x.Id,
            x.Name,
            x.SystemPrompt,
            x.UserPromptTemplate,
            x.StyleRulesJson,
            x.Model,
            x.Temperature,
            x.MaxTokens,
            x.IsActive,
            x.CreatedAt,
            x.UpdatedAt);

    private static PromptPresetDto ToDto(PromptPreset x) =>
        new(
            x.Id,
            x.Name,
            x.SystemPrompt,
            x.UserPromptTemplate,
            x.StyleRulesJson,
            x.Model,
            x.Temperature,
            x.MaxTokens,
            x.IsActive,
            x.CreatedAt,
            x.UpdatedAt);
}
