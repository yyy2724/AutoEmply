package health.autoemplyserver.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClaudeToolSchemas {

    public Map<String, Object> buildLayoutSpecTool() {
        return map(
            "name", "emit_layout_spec",
            "description", "Return only LayoutSpec JSON payload for Delphi QuickReport, preserving source layout detail, proportions, and colors.",
            "input_schema", map(
                "type", "object",
                "properties", map(
                    "items", map(
                        "type", "array",
                        "items", map(
                            "type", "object",
                            "properties", map(
                                "name", map("type", "string"),
                                "type", map("type", "string", "enum", List.of("Text", "Line", "Rect", "Image")),
                                "left", map("type", "integer"),
                                "top", map("type", "integer"),
                                "width", map("type", "integer"),
                                "height", map("type", "integer"),
                                "caption", map("type", "string"),
                                "align", map("type", "string", "enum", List.of("Left", "Center", "Right")),
                                "fontSize", map("type", "integer"),
                                "bold", map("type", "boolean"),
                                "transparent", map("type", "boolean"),
                                "textColor", map("type", "string"),
                                "orientation", map("type", "string", "enum", List.of("H", "V")),
                                "thickness", map("type", "integer"),
                                "strokeColor", map("type", "string"),
                                "fillColor", map("type", "string"),
                                "filled", map("type", "boolean"),
                                "stretch", map("type", "boolean")
                            ),
                            "required", List.of("type", "left", "top", "width", "height"),
                            "additionalProperties", false
                        )
                    ),
                    "pas", map(
                        "type", "object",
                        "properties", map(
                            "uses", map("type", "array", "items", map("type", "string")),
                            "methods", map(
                                "type", "array",
                                "items", map(
                                    "type", "object",
                                    "properties", map(
                                        "declaration", map("type", "string"),
                                        "body", map("type", "array", "items", map("type", "string"))
                                    ),
                                    "required", List.of("declaration", "body"),
                                    "additionalProperties", false
                                )
                            )
                        ),
                        "additionalProperties", false
                    )
                ),
                "required", List.of("items"),
                "additionalProperties", false
            )
        );
    }

    public Map<String, Object> buildFormStructureTool() {
        return map(
            "name", "emit_form_structure",
            "description", "Extract the logical structure of a Korean form from the image. Output sections, tables, rows, cells, and freeform elements using fractions instead of pixel coordinates.",
            "input_schema", map(
                "type", "object",
                "properties", map(
                    "title", map("type", "string"),
                    "titleFontSize", map("type", "integer"),
                    "sections", map(
                        "type", "array",
                        "items", map(
                            "type", "object",
                            "properties", map(
                                "sectionType", map("type", "string", "enum", List.of("table", "freeform", "keyvalue")),
                                "label", map("type", "string"),
                                "hasHeaderBackground", map("type", "boolean"),
                                "table", buildTableSchema(),
                                "elements", buildFreeformElementsSchema()
                            ),
                            "required", List.of("sectionType")
                        )
                    ),
                    "footer", buildFooterSchema()
                ),
                "required", List.of("sections")
            )
        );
    }

    private Map<String, Object> buildTableSchema() {
        return map(
            "type", "object",
            "properties", map(
                "columns", map(
                    "type", "array",
                    "items", map(
                        "type", "object",
                        "properties", map(
                            "widthFraction", map("type", "number"),
                            "header", map("type", "string"),
                            "headerColumn", map("type", "boolean"),
                            "isHeaderColumn", map("type", "boolean")
                        ),
                        "required", List.of("widthFraction")
                    )
                ),
                "rows", map(
                    "type", "array",
                    "items", map(
                        "type", "object",
                        "properties", map(
                            "heightHint", map("type", "string"),
                            "headerRow", map("type", "boolean"),
                            "isHeaderRow", map("type", "boolean"),
                            "cells", map(
                                "type", "array",
                                "items", map(
                                    "type", "object",
                                    "properties", map(
                                        "text", map("type", "string"),
                                        "align", map("type", "string", "enum", List.of("Left", "Center", "Right")),
                                        "bold", map("type", "boolean"),
                                        "fontSize", map("type", "integer"),
                                        "colSpan", map("type", "integer"),
                                        "rowSpan", map("type", "integer"),
                                        "hasBackground", map("type", "boolean"),
                                        "fieldName", map("type", "string"),
                                        "textColor", map("type", "string")
                                    ),
                                    "required", List.of("text")
                                )
                            )
                        ),
                        "required", List.of("cells")
                    )
                ),
                "fullWidth", map("type", "boolean"),
                "leftFraction", map("type", "number"),
                "widthFraction", map("type", "number")
            ),
            "required", List.of("columns", "rows")
        );
    }

    private Map<String, Object> buildFreeformElementsSchema() {
        return map(
            "type", "array",
            "items", map(
                "type", "object",
                "properties", map(
                    "elementType", map("type", "string", "enum", List.of("text", "checkbox", "image", "line")),
                    "text", map("type", "string"),
                    "xFraction", map("type", "number"),
                    "yFraction", map("type", "number"),
                    "widthFraction", map("type", "number"),
                    "heightFraction", map("type", "number"),
                    "align", map("type", "string"),
                    "bold", map("type", "boolean"),
                    "fontSize", map("type", "integer")
                ),
                "required", List.of("elementType", "text", "xFraction", "yFraction", "widthFraction")
            )
        );
    }

    private Map<String, Object> buildFooterSchema() {
        return map(
            "type", "array",
            "items", map(
                "type", "object",
                "properties", map(
                    "elementType", map("type", "string", "enum", List.of("text", "image", "signature_line")),
                    "text", map("type", "string"),
                    "align", map("type", "string"),
                    "bold", map("type", "boolean"),
                    "fontSize", map("type", "integer"),
                    "xFraction", map("type", "number"),
                    "widthFraction", map("type", "number")
                ),
                "required", List.of("elementType")
            )
        );
    }

    private Map<String, Object> map(Object... values) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], values[index + 1]);
        }
        return result;
    }
}
