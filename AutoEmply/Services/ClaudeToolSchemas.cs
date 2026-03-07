namespace AutoEmply.Services;

public static class ClaudeToolSchemas
{
    public static object BuildFormStructureTool()
    {
        return new
        {
            name = "emit_form_structure",
            description = "Extract the logical structure of a Korean form from the image. Output sections, tables, rows, cells, and freeform elements using fractions (0.0-1.0) instead of pixel coordinates.",
            input_schema = new
            {
                type = "object",
                properties = new
                {
                    title = new { type = "string", description = "Main title text of the form" },
                    titleFontSize = new { type = "integer", description = "Font size for title. Default 20." },
                    sections = new
                    {
                        type = "array",
                        items = new
                        {
                            type = "object",
                            properties = new
                            {
                                sectionType = new { type = "string", @enum = new[] { "table", "freeform", "keyvalue" } },
                                label = new { type = "string", description = "Optional section label" },
                                hasHeaderBackground = new { type = "boolean" },
                                table = new
                                {
                                    type = "object",
                                    properties = new
                                    {
                                        columns = new
                                        {
                                            type = "array",
                                            items = new
                                            {
                                                type = "object",
                                                properties = new
                                                {
                                                    widthFraction = new { type = "number", description = "Fraction of table width (0.0-1.0). All columns must sum to 1.0." },
                                                    header = new { type = "string" },
                                                    isHeaderColumn = new { type = "boolean", description = "True if this column has colored background" }
                                                },
                                                required = new[] { "widthFraction" }
                                            }
                                        },
                                        rows = new
                                        {
                                            type = "array",
                                            items = new
                                            {
                                                type = "object",
                                                properties = new
                                                {
                                                    heightHint = new { type = "string", description = "standard(20px), compact(14px), tall(30px), or integer" },
                                                    isHeaderRow = new { type = "boolean" },
                                                    cells = new
                                                    {
                                                        type = "array",
                                                        items = new
                                                        {
                                                            type = "object",
                                                            properties = new
                                                            {
                                                                text = new { type = "string" },
                                                                align = new { type = "string", @enum = new[] { "Left", "Center", "Right" } },
                                                                bold = new { type = "boolean" },
                                                                fontSize = new { type = "integer" },
                                                                colSpan = new { type = "integer", description = "Number of columns this cell spans. Default 1." },
                                                                rowSpan = new { type = "integer" },
                                                                hasBackground = new { type = "boolean", description = "True if cell has colored background" },
                                                                fieldName = new { type = "string", description = "Variable/placeholder name if present" },
                                                                textColor = new { type = "string" }
                                                            },
                                                            required = new[] { "text" }
                                                        }
                                                    }
                                                },
                                                required = new[] { "cells" }
                                            }
                                        },
                                        fullWidth = new { type = "boolean" },
                                        leftFraction = new { type = "number" },
                                        widthFraction = new { type = "number" }
                                    },
                                    required = new[] { "columns", "rows" }
                                },
                                elements = new
                                {
                                    type = "array",
                                    items = new
                                    {
                                        type = "object",
                                        properties = new
                                        {
                                            elementType = new { type = "string", @enum = new[] { "text", "checkbox", "image", "line" } },
                                            text = new { type = "string" },
                                            xFraction = new { type = "number" },
                                            yFraction = new { type = "number" },
                                            widthFraction = new { type = "number" },
                                            heightFraction = new { type = "number" },
                                            align = new { type = "string" },
                                            bold = new { type = "boolean" },
                                            fontSize = new { type = "integer" }
                                        },
                                        required = new[] { "elementType", "text", "xFraction", "yFraction", "widthFraction" }
                                    }
                                }
                            },
                            required = new[] { "sectionType" }
                        }
                    },
                    footer = new
                    {
                        type = "array",
                        items = new
                        {
                            type = "object",
                            properties = new
                            {
                                elementType = new { type = "string", @enum = new[] { "text", "image", "signature_line" } },
                                text = new { type = "string" },
                                align = new { type = "string" },
                                bold = new { type = "boolean" },
                                fontSize = new { type = "integer" },
                                xFraction = new { type = "number" },
                                widthFraction = new { type = "number" }
                            },
                            required = new[] { "elementType" }
                        }
                    }
                },
                required = new[] { "sections" }
            }
        };
    }
}
