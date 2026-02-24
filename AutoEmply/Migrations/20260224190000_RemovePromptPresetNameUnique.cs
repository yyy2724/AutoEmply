using AutoEmply.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AutoEmply.Migrations
{
    [DbContext(typeof(AppDbContext))]
    [Migration("20260224190000_RemovePromptPresetNameUnique")]
    public partial class RemovePromptPresetNameUnique : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_prompt_presets_name",
                table: "prompt_presets");

            migrationBuilder.CreateIndex(
                name: "IX_prompt_presets_name",
                table: "prompt_presets",
                column: "name");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_prompt_presets_name",
                table: "prompt_presets");

            migrationBuilder.CreateIndex(
                name: "IX_prompt_presets_name",
                table: "prompt_presets",
                column: "name",
                unique: true);
        }
    }
}
