using AutoEmply.Entities;
using Microsoft.EntityFrameworkCore;

namespace AutoEmply.Data;

public sealed class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<PromptPreset> PromptPresets => Set<PromptPreset>();
    public DbSet<PromptVersion> PromptVersions => Set<PromptVersion>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<PromptPreset>(entity =>
        {
            entity.ToTable("prompt_presets");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.Id).HasColumnName("id");
            entity.Property(x => x.Name).HasColumnName("name").IsRequired();
            entity.HasIndex(x => x.Name);
            entity.Property(x => x.SystemPrompt).HasColumnName("system_prompt").IsRequired();
            entity.Property(x => x.UserPromptTemplate).HasColumnName("user_prompt_template").IsRequired();
            entity.Property(x => x.StyleRulesJson).HasColumnName("style_rules_json").HasColumnType("jsonb");
            entity.Property(x => x.Model).HasColumnName("model");
            entity.Property(x => x.Temperature).HasColumnName("temperature");
            entity.Property(x => x.MaxTokens).HasColumnName("max_tokens");
            entity.Property(x => x.IsActive).HasColumnName("is_active").HasDefaultValue(true);
            entity.Property(x => x.CreatedAt).HasColumnName("created_at");
            entity.Property(x => x.UpdatedAt).HasColumnName("updated_at");
        });

        modelBuilder.Entity<PromptVersion>(entity =>
        {
            entity.ToTable("prompt_versions");
            entity.HasKey(x => x.Id);
            entity.Property(x => x.Id).HasColumnName("id");
            entity.Property(x => x.PresetId).HasColumnName("preset_id");
            entity.Property(x => x.Version).HasColumnName("version");
            entity.Property(x => x.SystemPrompt).HasColumnName("system_prompt").IsRequired();
            entity.Property(x => x.UserPromptTemplate).HasColumnName("user_prompt_template").IsRequired();
            entity.Property(x => x.StyleRulesJson).HasColumnName("style_rules_json").HasColumnType("jsonb");
            entity.Property(x => x.CreatedAt).HasColumnName("created_at");
            entity.HasIndex(x => new { x.PresetId, x.Version }).IsUnique();
            entity.HasOne(x => x.Preset)
                .WithMany(x => x.Versions)
                .HasForeignKey(x => x.PresetId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
