using AutoEmply.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AutoEmply.Migrations
{
    [DbContext(typeof(AppDbContext))]
    [Migration("20260225103000_BackfillActivePresetMaxTokens8192")]
    public partial class BackfillActivePresetMaxTokens8192 : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(
                @"UPDATE prompt_presets
                  SET max_tokens = 8192
                  WHERE is_active = TRUE;");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(
                @"UPDATE prompt_presets
                  SET max_tokens = 2048
                  WHERE is_active = TRUE AND max_tokens = 8192;");
        }
    }
}
