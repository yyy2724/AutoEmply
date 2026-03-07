namespace AutoEmply.Services;

/// <summary>
/// Claude API의 tool_use에 전달하는 JSON Schema 정의.
/// 각 메서드는 Claude에게 "이 스키마에 맞는 JSON을 반환하라"고 지시하는 도구 정의를 만든다.
/// </summary>
public static class ClaudeToolSchemas
{
    /// <summary>
    /// LayoutSpec 직접 생성용 도구.
    /// 픽셀 좌표 기반의 Text/Line/Rect/Image 아이템 배열을 반환하도록 한다.
    /// </summary>
    public static object BuildLayoutSpecTool() => new
    {
        name = "emit_layout_spec",
        description = "Return only LayoutSpec JSON payload for Delphi QuickReport, preserving source layout detail, proportions, and colors.",
        input_schema = new
        {
            type = "object",
            properties = new
            {
                items = new
                {
                    type = "array",
                    items = new
                    {
                        type = "object",
                        properties = new
                        {
                            name = new { type = "string" },
                            type = new { type = "string", @enum = new[] { "Text", "Line", "Rect", "Image" } },
                            left = new { type = "integer" },
                            top = new { type = "integer" },
                            width = new { type = "integer" },
                            height = new { type = "integer" },
                            caption = new { type = "string" },
                            align = new { type = "string", @enum = new[] { "Left", "Center", "Right" } },
                            fontSize = new { type = "integer" },
                            bold = new { type = "boolean" },
                            transparent = new { type = "boolean" },
                            textColor = new { type = "string" },
                            orientation = new { type = "string", @enum = new[] { "H", "V" } },
                            thickness = new { type = "integer" },
                            strokeColor = new { type = "string" },
                            fillColor = new { type = "string" },
                            filled = new { type = "boolean" },
                            stretch = new { type = "boolean" }
                        },
                        required = new[] { "type", "left", "top", "width", "height" },
                        additionalProperties = false
                    }
                },
                pas = new
                {
                    type = "object",
                    properties = new
                    {
                        uses = new { type = "array", items = new { type = "string" } },
                        methods = new
                        {
                            type = "array",
                            items = new
                            {
                                type = "object",
                                properties = new
                                {
                                    declaration = new { type = "string" },
                                    body = new { type = "array", items = new { type = "string" } }
                                },
                                required = new[] { "declaration", "body" },
                                additionalProperties = false
                            }
                        }
                    },
                    additionalProperties = false
                }
            },
            required = new[] { "items" },
            additionalProperties = false
        }
    };

    /// <summary>
    /// FormStructure 추출용 도구 (Phase 1).
    /// 비율 기반의 논리 구조(섹션, 테이블, 자유 배치 요소)를 반환하도록 한다.
    /// </summary>
    public static object BuildFormStructureTool() => new
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
                            table = BuildTableSchema(),
                            elements = BuildFreeformElementsSchema()
                        },
                        required = new[] { "sectionType" }
                    }
                },
                footer = BuildFooterSchema()
            },
            required = new[] { "sections" }
        }
    };

    // ── 하위 스키마 빌더 ──

    private static object BuildTableSchema() => new
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
    };

    private static object BuildFreeformElementsSchema() => new
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
    };

    private static object BuildFooterSchema() => new
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
    };
}
