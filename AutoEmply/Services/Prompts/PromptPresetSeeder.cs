using AutoEmply.Data;

namespace AutoEmply.Services.Prompts;

public sealed class PromptPresetSeeder(AppDbContext dbContext)
{
    public async Task SeedAsync(CancellationToken cancellationToken)
    {
        _ = dbContext;
        await Task.CompletedTask;
    }
}
