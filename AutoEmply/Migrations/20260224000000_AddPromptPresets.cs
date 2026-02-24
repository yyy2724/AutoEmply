using System;
using AutoEmply.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AutoEmply.Migrations
{
    [DbContext(typeof(AppDbContext))]
    [Migration("20260224000000_AddPromptPresets")]
    public partial class AddPromptPresets : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "prompt_presets",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    name = table.Column<string>(type: "text", nullable: false),
                    system_prompt = table.Column<string>(type: "text", nullable: false),
                    user_prompt_template = table.Column<string>(type: "text", nullable: false),
                    style_rules_json = table.Column<string>(type: "jsonb", nullable: true),
                    model = table.Column<string>(type: "text", nullable: true),
                    temperature = table.Column<decimal>(type: "numeric", nullable: true),
                    max_tokens = table.Column<int>(type: "integer", nullable: true),
                    is_active = table.Column<bool>(type: "boolean", nullable: false, defaultValue: true),
                    created_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    updated_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_prompt_presets", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "prompt_versions",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    preset_id = table.Column<Guid>(type: "uuid", nullable: false),
                    version = table.Column<int>(type: "integer", nullable: false),
                    system_prompt = table.Column<string>(type: "text", nullable: false),
                    user_prompt_template = table.Column<string>(type: "text", nullable: false),
                    style_rules_json = table.Column<string>(type: "jsonb", nullable: true),
                    created_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_prompt_versions", x => x.id);
                    table.ForeignKey(
                        name: "FK_prompt_versions_prompt_presets_preset_id",
                        column: x => x.preset_id,
                        principalTable: "prompt_presets",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_prompt_presets_name",
                table: "prompt_presets",
                column: "name",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_prompt_versions_preset_id_version",
                table: "prompt_versions",
                columns: new[] { "preset_id", "version" },
                unique: true);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "prompt_versions");

            migrationBuilder.DropTable(
                name: "prompt_presets");
        }
    }
}
