using AutoEmply.Data;
using AutoEmply.Dtos;
using AutoEmply.Entities;
using Microsoft.EntityFrameworkCore;

namespace AutoEmply.Services.Prompts;

public sealed class PromptPresetService(AppDbContext dbContext, IConfiguration configuration)
{
    private const string DefaultModel = "claude-sonnet-4-6";
    private const decimal DefaultTemperature = 0m;
    private const int DefaultMaxTokens = 2048;
    private const int PastPromptReferenceCount = 20;

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

            return new ResolvedPromptPreset(
                specificPreset.Id,
                specificPreset.Name,
                specificPreset.SystemPrompt,
                specificPreset.UserPromptTemplate,
                specificPreset.StyleRulesJson,
                ResolveModel(specificPreset.Model),
                specificPreset.Temperature ?? DefaultTemperature,
                specificPreset.MaxTokens ?? DefaultMaxTokens);
        }

        var primaryPreset = await dbContext.PromptPresets
            .AsNoTracking()
            .Where(x => x.IsActive)
            .OrderByDescending(x => x.UpdatedAt)
            .FirstOrDefaultAsync(cancellationToken);
        if (primaryPreset is null)
        {
            return null;
        }

        var pastPrompts = await dbContext.PromptPresets
            .AsNoTracking()
            .Where(x => x.IsActive && x.Id != primaryPreset.Id)
            .OrderByDescending(x => x.UpdatedAt)
            .Take(PastPromptReferenceCount)
            .Select(x => x.SystemPrompt)
            .ToListAsync(cancellationToken);

        var mergedSystemPrompt = BuildSystemPromptWithPastPrompts(primaryPreset.SystemPrompt, pastPrompts);

        return new ResolvedPromptPreset(
            primaryPreset.Id,
            primaryPreset.Name,
            mergedSystemPrompt,
            primaryPreset.UserPromptTemplate,
            primaryPreset.StyleRulesJson,
            ResolveModel(primaryPreset.Model),
            primaryPreset.Temperature ?? DefaultTemperature,
            primaryPreset.MaxTokens ?? DefaultMaxTokens);
    }

    private static string BuildSystemPromptWithPastPrompts(string currentPrompt, IReadOnlyList<string> pastPrompts)
    {
        if (pastPrompts.Count == 0)
        {
            return currentPrompt;
        }

        var lines = new List<string>
        {
            currentPrompt.Trim(),
            "",
            "PastPromptReference:",
            "Use these as secondary context only if they do not conflict with the current prompt."
        };

        for (var i = 0; i < pastPrompts.Count; i++)
        {
            lines.Add($"[{i + 1}] {pastPrompts[i].Trim()}");
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
