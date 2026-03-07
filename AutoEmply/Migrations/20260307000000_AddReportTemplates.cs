using System;
using AutoEmply.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AutoEmply.Migrations
{
    [DbContext(typeof(AppDbContext))]
    [Migration("20260307000000_AddReportTemplates")]
    public partial class AddReportTemplates : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "report_templates",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    name = table.Column<string>(type: "text", nullable: false),
                    category = table.Column<string>(type: "text", nullable: false),
                    dfm_content = table.Column<string>(type: "text", nullable: false),
                    pas_content = table.Column<string>(type: "text", nullable: false),
                    original_form_name = table.Column<string>(type: "text", nullable: false),
                    preview_content_type = table.Column<string>(type: "text", nullable: true),
                    preview_data = table.Column<byte[]>(type: "bytea", nullable: true),
                    created_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    updated_at = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_report_templates", x => x.id);
                });

            migrationBuilder.CreateIndex(
                name: "IX_report_templates_category",
                table: "report_templates",
                column: "category");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(name: "report_templates");
        }
    }
}
