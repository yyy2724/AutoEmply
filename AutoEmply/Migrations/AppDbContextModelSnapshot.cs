using System;
using AutoEmply.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;

#nullable disable

namespace AutoEmply.Migrations
{
    [DbContext(typeof(AppDbContext))]
    partial class AppDbContextModelSnapshot : ModelSnapshot
    {
        protected override void BuildModel(ModelBuilder modelBuilder)
        {
            modelBuilder
                .HasAnnotation("ProductVersion", "9.0.4");

            modelBuilder.Entity("AutoEmply.Entities.ReportTemplate", b =>
                {
                    b.Property<Guid>("Id")
                        .HasColumnType("uuid")
                        .HasColumnName("id");

                    b.Property<string>("Name")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("name");

                    b.Property<string>("Category")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("category");

                    b.Property<string>("DfmContent")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("dfm_content");

                    b.Property<string>("PasContent")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("pas_content");

                    b.Property<string>("OriginalFormName")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("original_form_name");

                    b.Property<string>("PreviewContentType")
                        .HasColumnType("text")
                        .HasColumnName("preview_content_type");

                    b.Property<byte[]>("PreviewData")
                        .HasColumnType("bytea")
                        .HasColumnName("preview_data");

                    b.Property<DateTimeOffset>("CreatedAt")
                        .HasColumnType("timestamp with time zone")
                        .HasColumnName("created_at");

                    b.Property<DateTimeOffset>("UpdatedAt")
                        .HasColumnType("timestamp with time zone")
                        .HasColumnName("updated_at");

                    b.HasKey("Id");

                    b.HasIndex("Category");

                    b.ToTable("report_templates", (string)null);
                });

            modelBuilder.Entity("AutoEmply.Entities.PromptPreset", b =>
                {
                    b.Property<Guid>("Id")
                        .HasColumnType("uuid")
                        .HasColumnName("id");

                    b.Property<DateTimeOffset>("CreatedAt")
                        .HasColumnType("timestamp with time zone")
                        .HasColumnName("created_at");

                    b.Property<bool>("IsActive")
                        .ValueGeneratedOnAdd()
                        .HasColumnType("boolean")
                        .HasColumnName("is_active")
                        .HasDefaultValue(true);

                    b.Property<int?>("MaxTokens")
                        .HasColumnType("integer")
                        .HasColumnName("max_tokens");

                    b.Property<string>("Model")
                        .HasColumnType("text")
                        .HasColumnName("model");

                    b.Property<string>("Name")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("name");

                    b.Property<string>("StyleRulesJson")
                        .HasColumnType("jsonb")
                        .HasColumnName("style_rules_json");

                    b.Property<string>("SystemPrompt")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("system_prompt");

                    b.Property<decimal?>("Temperature")
                        .HasColumnType("numeric")
                        .HasColumnName("temperature");

                    b.Property<DateTimeOffset>("UpdatedAt")
                        .HasColumnType("timestamp with time zone")
                        .HasColumnName("updated_at");

                    b.Property<string>("UserPromptTemplate")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("user_prompt_template");

                    b.HasKey("Id");

                    b.HasIndex("Name");

                    b.ToTable("prompt_presets", (string)null);
                });

            modelBuilder.Entity("AutoEmply.Entities.PromptVersion", b =>
                {
                    b.Property<Guid>("Id")
                        .HasColumnType("uuid")
                        .HasColumnName("id");

                    b.Property<DateTimeOffset>("CreatedAt")
                        .HasColumnType("timestamp with time zone")
                        .HasColumnName("created_at");

                    b.Property<Guid>("PresetId")
                        .HasColumnType("uuid")
                        .HasColumnName("preset_id");

                    b.Property<string>("StyleRulesJson")
                        .HasColumnType("jsonb")
                        .HasColumnName("style_rules_json");

                    b.Property<string>("SystemPrompt")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("system_prompt");

                    b.Property<string>("UserPromptTemplate")
                        .IsRequired()
                        .HasColumnType("text")
                        .HasColumnName("user_prompt_template");

                    b.Property<int>("Version")
                        .HasColumnType("integer")
                        .HasColumnName("version");

                    b.HasKey("Id");

                    b.HasIndex("PresetId", "Version")
                        .IsUnique();

                    b.ToTable("prompt_versions", (string)null);
                });

            modelBuilder.Entity("AutoEmply.Entities.PromptVersion", b =>
                {
                    b.HasOne("AutoEmply.Entities.PromptPreset", "Preset")
                        .WithMany("Versions")
                        .HasForeignKey("PresetId")
                        .OnDelete(DeleteBehavior.Cascade)
                        .IsRequired();

                    b.Navigation("Preset");
                });

            modelBuilder.Entity("AutoEmply.Entities.PromptPreset", b =>
                {
                    b.Navigation("Versions");
                });
        }
    }
}
