package health.autoemplyserver.service;

import health.autoemplyserver.model.ColumnDef;
import health.autoemplyserver.model.FooterElement;
import health.autoemplyserver.model.FormSection;
import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.FreeformElement;
import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.model.TableCell;
import health.autoemplyserver.model.TableDef;
import health.autoemplyserver.model.TableRow;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class StructureToLayoutConverter {

    private static final int CANVAS_LEFT = 10;
    private static final int CONTENT_WIDTH = 774;
    private static final int STANDARD_ROW_HEIGHT = 20;
    private static final int COMPACT_ROW_HEIGHT = 14;
    private static final int TALL_ROW_HEIGHT = 30;
    private static final int LABEL_HEIGHT = 13;
    private static final int TITLE_HEIGHT = 28;
    private static final int CELL_PADDING_LEFT = 4;
    private static final int CELL_PADDING_TOP = 3;
    private static final int SECTION_GAP = 2;
    private static final String HEADER_BG_COLOR = "#D8E4F0";
    private static final String BORDER_COLOR = "#000000";
    private static final Pattern FIELD_CODE_REGEX = Pattern.compile("([A-Za-z]{2,4}\\d{2,4})");

    public LayoutSpec convert(FormStructure structure) {
        List<LayoutItem> items = new ArrayList<>();
        int currentY = 6;

        if (structure.getTitle() != null && !structure.getTitle().isBlank()) {
            items.add(makeLabel(structure.getTitle(), CANVAS_LEFT + CONTENT_WIDTH / 4, currentY, CONTENT_WIDTH / 2, TITLE_HEIGHT,
                structure.getTitleFontSize(), true, "Center", "#000000", "Qlb3_TITLE"));
            currentY += TITLE_HEIGHT + 6;
        }

        for (FormSection section : structure.getSections()) {
            currentY = renderSection(section, items, currentY);
            currentY += SECTION_GAP;
        }

        if (structure.getFooter() != null && !structure.getFooter().isEmpty()) {
            currentY += 4;
            renderFooter(structure.getFooter(), items, currentY);
        }

        LayoutSpec layoutSpec = new LayoutSpec();
        layoutSpec.setItems(items);
        return layoutSpec;
    }

    private int renderSection(FormSection section, List<LayoutItem> items, int startY) {
        String sectionType = section.getSectionType() == null ? "table" : section.getSectionType().toLowerCase();
        if ("table".equals(sectionType)) {
            return renderTableSection(section, items, startY);
        }
        return renderFreeformSection(section, items, startY);
    }

    private int renderTableSection(FormSection section, List<LayoutItem> items, int startY) {
        TableDef table = section.getTable();
        if (table == null || table.getColumns().isEmpty() || table.getRows().isEmpty()) {
            return startY;
        }

        int tableLeft = table.isFullWidth() ? CANVAS_LEFT : CANVAS_LEFT + (int) Math.round(table.getLeftFraction() * CONTENT_WIDTH);
        int tableWidth = table.isFullWidth() ? CONTENT_WIDTH : (int) Math.round(table.getWidthFraction() * CONTENT_WIDTH);
        int[] colBounds = calculateColumnBoundaries(table.getColumns(), tableLeft, tableWidth);
        List<Integer> rowYs = calculateRowPositions(table.getRows(), startY);
        int totalHeight = rowYs.getLast() - startY;

        renderBackgrounds(table, items, colBounds, rowYs, tableLeft, tableWidth, startY, totalHeight);
        renderBorders(table, items, colBounds, rowYs, tableLeft, tableWidth, startY, totalHeight);
        renderCellTexts(table, items, colBounds, rowYs);

        return rowYs.getLast();
    }

    private void renderBackgrounds(TableDef table, List<LayoutItem> items, int[] colBounds, List<Integer> rowYs, int tableLeft, int tableWidth, int startY, int totalHeight) {
        for (int col = 0; col < table.getColumns().size(); col++) {
            if (table.getColumns().get(col).isHeaderColumn()) {
                items.add(makeRect(colBounds[col] + 1, startY + 1, colBounds[col + 1] - colBounds[col] - 1, totalHeight - 1, HEADER_BG_COLOR));
            }
        }

        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            TableRow row = table.getRows().get(rowIndex);
            int rowHeight = rowYs.get(rowIndex + 1) - rowYs.get(rowIndex);
            if (row.isHeaderRow()) {
                items.add(makeRect(tableLeft + 1, rowYs.get(rowIndex) + 1, tableWidth - 1, rowHeight - 1, HEADER_BG_COLOR));
                continue;
            }
            int colIndex = 0;
            for (TableCell cell : row.getCells()) {
                if (cell.isHasBackground() && colIndex < colBounds.length - 1) {
                    int spanEnd = Math.min(colIndex + Math.max(1, cell.getColSpan()), colBounds.length - 1);
                    items.add(makeRect(colBounds[colIndex] + 1, rowYs.get(rowIndex) + 1,
                        colBounds[spanEnd] - colBounds[colIndex] - 1, rowHeight - 1, HEADER_BG_COLOR));
                }
                colIndex += Math.max(1, cell.getColSpan());
            }
        }
    }

    private void renderBorders(TableDef table, List<LayoutItem> items, int[] colBounds, List<Integer> rowYs, int tableLeft, int tableWidth, int startY, int totalHeight) {
        for (int row = 0; row <= table.getRows().size(); row++) {
            items.add(makeHLine(tableLeft, rowYs.get(row), tableWidth));
        }
        for (int col = 0; col <= table.getColumns().size(); col++) {
            int x = col < colBounds.length ? colBounds[col] : tableLeft + tableWidth;
            items.add(makeVLine(x, startY, totalHeight));
        }
    }

    private void renderCellTexts(TableDef table, List<LayoutItem> items, int[] colBounds, List<Integer> rowYs) {
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            int colIndex = 0;
            for (TableCell cell : table.getRows().get(rowIndex).getCells()) {
                if (colIndex >= colBounds.length - 1) {
                    break;
                }
                int spanEnd = Math.min(colIndex + Math.max(1, cell.getColSpan()), colBounds.length - 1);
                int cellLeft = colBounds[colIndex] + CELL_PADDING_LEFT;
                int cellTop = rowYs.get(rowIndex) + CELL_PADDING_TOP;
                int cellWidth = colBounds[spanEnd] - colBounds[colIndex] - CELL_PADDING_LEFT * 2;
                String displayText = resolveDisplayText(cell.getText(), cell.getFieldName());
                if (displayText != null && !displayText.isBlank()) {
                    items.add(makeLabel(displayText, cellLeft, cellTop, Math.max(1, cellWidth), LABEL_HEIGHT,
                        cell.getFontSize() > 0 ? cell.getFontSize() : 9, cell.isBold(), cell.getAlign(), cell.getTextColor(), resolveComponentName(displayText, cell.getFieldName())));
                }
                colIndex += Math.max(1, cell.getColSpan());
            }
        }
    }

    private int renderFreeformSection(FormSection section, List<LayoutItem> items, int startY) {
        if (section.getElements() == null || section.getElements().isEmpty()) {
            return startY;
        }

        double maxBottom = section.getElements().stream().mapToDouble(element -> element.getYFraction() + element.getHeightFraction()).max().orElse(0.2d);
        int sectionHeight = Math.max(STANDARD_ROW_HEIGHT, (int) Math.round(maxBottom * 200));

        for (FreeformElement element : section.getElements()) {
            int left = CANVAS_LEFT + (int) Math.round(element.getXFraction() * CONTENT_WIDTH);
            int top = startY + (int) Math.round(element.getYFraction() * sectionHeight);
            int width = Math.max(1, (int) Math.round(element.getWidthFraction() * CONTENT_WIDTH));
            int height = Math.max(LABEL_HEIGHT, (int) Math.round(element.getHeightFraction() * sectionHeight));

            switch ((element.getElementType() == null ? "text" : element.getElementType()).toLowerCase()) {
                case "line" -> items.add(makeHLine(left, top, width));
                case "image" -> {
                    LayoutItem item = new LayoutItem();
                    item.setType("Image");
                    item.setLeft(left);
                    item.setTop(top);
                    item.setWidth(width);
                    item.setHeight(height);
                    item.setStretch(true);
                    items.add(item);
                }
                default -> items.add(makeLabel(element.getText() == null ? "" : element.getText(), left, top, width, LABEL_HEIGHT,
                    element.getFontSize(), element.isBold(), element.getAlign(), "#000000", null));
            }
        }
        return startY + sectionHeight;
    }

    private void renderFooter(List<FooterElement> footerElements, List<LayoutItem> items, int startY) {
        int y = startY;
        for (FooterElement element : footerElements) {
            int left = CANVAS_LEFT + (int) Math.round(element.getXFraction() * CONTENT_WIDTH);
            int width = Math.max(1, (int) Math.round(element.getWidthFraction() * CONTENT_WIDTH));
            switch ((element.getElementType() == null ? "text" : element.getElementType()).toLowerCase()) {
                case "image" -> {
                    LayoutItem image = new LayoutItem();
                    image.setType("Image");
                    image.setLeft(left);
                    image.setTop(y);
                    image.setWidth(width);
                    image.setHeight(30);
                    image.setStretch(true);
                    items.add(image);
                    y += 34;
                }
                case "signature_line" -> {
                    items.add(makeHLine(left, y + 10, width));
                    y += 14;
                }
                default -> {
                    items.add(makeLabel(element.getText() == null ? "" : element.getText(), left, y, width, LABEL_HEIGHT, element.getFontSize(), element.isBold(), element.getAlign(), "#000000", null));
                    y += LABEL_HEIGHT + 2;
                }
            }
        }
    }

    private int[] calculateColumnBoundaries(List<ColumnDef> columns, int tableLeft, int tableWidth) {
        int[] bounds = new int[columns.size() + 1];
        bounds[0] = tableLeft;
        double total = columns.stream().mapToDouble(ColumnDef::getWidthFraction).sum();
        double factor = total > 0 ? 1.0d / total : 1.0d;
        double accum = 0;
        for (int index = 0; index < columns.size(); index++) {
            accum += columns.get(index).getWidthFraction() * factor;
            bounds[index + 1] = tableLeft + (int) Math.round(accum * tableWidth);
        }
        bounds[columns.size()] = tableLeft + tableWidth;
        return bounds;
    }

    private List<Integer> calculateRowPositions(List<TableRow> rows, int startY) {
        List<Integer> positions = new ArrayList<>();
        positions.add(startY);
        for (TableRow row : rows) {
            positions.add(positions.getLast() + resolveRowHeight(row));
        }
        return positions;
    }

    private int resolveRowHeight(TableRow row) {
        String hint = row.getHeightHint() == null ? "standard" : row.getHeightHint().trim().toLowerCase();
        return switch (hint) {
            case "compact" -> COMPACT_ROW_HEIGHT;
            case "tall" -> TALL_ROW_HEIGHT;
            case "standard" -> STANDARD_ROW_HEIGHT;
            default -> {
                try {
                    yield Math.max(10, Math.min(300, Integer.parseInt(hint)));
                } catch (NumberFormatException exception) {
                    yield STANDARD_ROW_HEIGHT;
                }
            }
        };
    }

    private String resolveDisplayText(String text, String fieldName) {
        if (text != null && !text.trim().isBlank()) {
            return text.trim();
        }
        return fieldName == null ? "" : fieldName.trim();
    }

    private String resolveComponentName(String displayText, String fieldName) {
        if (fieldName != null && !fieldName.isBlank()) {
            Matcher matcher = FIELD_CODE_REGEX.matcher(fieldName.trim());
            if (matcher.find()) {
                return "Qlb3_" + matcher.group(1).toUpperCase();
            }
        }
        if (displayText == null || displayText.isBlank()) {
            return null;
        }
        String candidate = displayText.replaceAll("[^A-Za-z0-9]", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        return candidate.isBlank() ? null : "Qlb3_" + candidate.substring(0, Math.min(40, candidate.length()));
    }

    private LayoutItem makeLabel(String caption, int left, int top, int width, int height, int fontSize, boolean bold, String align, String textColor, String name) {
        LayoutItem item = new LayoutItem();
        item.setName(name);
        item.setType("Text");
        item.setLeft(Math.max(0, left));
        item.setTop(Math.max(0, top));
        item.setWidth(Math.max(1, width));
        item.setHeight(Math.max(1, height));
        item.setCaption(caption);
        item.setFontSize(Math.max(6, Math.min(24, fontSize)));
        item.setBold(bold);
        item.setAlign(align == null ? "Left" : align);
        item.setTransparent(true);
        item.setTextColor(textColor);
        return item;
    }

    private LayoutItem makeHLine(int left, int top, int width) {
        LayoutItem item = new LayoutItem();
        item.setType("Line");
        item.setLeft(Math.max(0, left));
        item.setTop(Math.max(0, top));
        item.setWidth(Math.max(1, width));
        item.setHeight(1);
        item.setOrientation("H");
        item.setThickness(1);
        item.setStrokeColor(BORDER_COLOR);
        return item;
    }

    private LayoutItem makeVLine(int left, int top, int height) {
        LayoutItem item = new LayoutItem();
        item.setType("Line");
        item.setLeft(Math.max(0, left));
        item.setTop(Math.max(0, top));
        item.setWidth(1);
        item.setHeight(Math.max(1, height));
        item.setOrientation("V");
        item.setThickness(1);
        item.setStrokeColor(BORDER_COLOR);
        return item;
    }

    private LayoutItem makeRect(int left, int top, int width, int height, String fillColor) {
        LayoutItem item = new LayoutItem();
        item.setType("Rect");
        item.setLeft(Math.max(0, left));
        item.setTop(Math.max(0, top));
        item.setWidth(Math.max(1, width));
        item.setHeight(Math.max(1, height));
        item.setFillColor(fillColor);
        item.setFilled(true);
        item.setStrokeColor(fillColor);
        item.setThickness(1);
        return item;
    }
}
