package health.autoemplyserver.service;

import static health.autoemplyserver.service.LayoutItemKinds.isHorizontalLine;
import static health.autoemplyserver.service.LayoutItemKinds.isLine;
import static health.autoemplyserver.service.LayoutItemKinds.isText;
import static health.autoemplyserver.service.LayoutItemKinds.isVerticalLine;
import static health.autoemplyserver.service.LayoutItemKinds.normalizeOrientation;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Cleans up a raw AI-generated {@link LayoutSpec} before Delphi code generation by running
 * a fixed 9-step pipeline. This class is a thin orchestrator: geometry snapping/alignment
 * is delegated to {@link LayoutGeometryNormalizer}, table border completion to
 * {@link TableBorderCompleter}, and item classification to {@link LayoutItemKinds}; the
 * remaining steps it owns directly are per-item invariants, canvas clamping, duplicate
 * border removal and z-ordering.
 */
@Component
public class LayoutPostProcessor {

    // Canvas geometry (pixels). Items are clamped into this drawable area.
    private static final int CANVAS_LEFT = 10;      // left edge of the drawable area in px
    private static final int CANVAS_RIGHT = 784;    // right edge of the drawable area in px
    private static final int CANVAS_TOP = 0;        // top edge of the drawable area in px
    private static final int CANVAS_BOTTOM = 1600;  // bottom edge of the drawable area in px

    // Style clamps.
    private static final int MIN_FONT_SIZE = 6;        // pt; smallest font size kept after clamping
    private static final int MAX_FONT_SIZE = 24;       // pt; largest font size kept after clamping
    private static final int MAX_LINE_THICKNESS = 6;   // px; largest pen thickness kept after clamping

    private final LayoutGeometryNormalizer geometryNormalizer = new LayoutGeometryNormalizer();
    private final TableBorderCompleter tableBorderCompleter = new TableBorderCompleter();

    public LayoutSpec process(LayoutSpec input) {
        if (input == null) {
            return null;
        }

        List<LayoutItem> items = new ArrayList<>();
        for (LayoutItem item : input.getItems() == null ? List.<LayoutItem>of() : input.getItems()) {
            items.add(item == null ? new LayoutItem() : item.copy());
        }

        // 1. Snap edges onto the dominant x/y grid inferred from coordinates used twice or more.
        items = geometryNormalizer.snapToGrid(items);
        // 2. Merge near-identical edge coordinates (top/left/right/bottom) to a single canonical value.
        items = geometryNormalizer.alignEdges(items);
        // 3. Normalize text heights to the standard label height and force lines to 1px thickness.
        items = geometryNormalizer.normalizeTextAndLineSizes(items);
        // 4. Detect table regions from horizontal lines and add/extend missing left/right border lines.
        //    Must run after 1-3 so region detection sees cleaned coordinates.
        items = tableBorderCompleter.completeBorders(items);
        // 5. Snap all line endpoints onto the grid formed by the crossing lines (closes corner gaps).
        items = geometryNormalizer.normalizeLineGrid(items);
        // 6. Enforce per-item invariants: transparent text, font/thickness clamps, non-negative geometry.
        items = enforceConsistency(items);
        // 7. Clamp every item into the drawable canvas area.
        items = enforceCanvasBounds(items);
        // 8. Drop duplicate border lines (possible after snapping/border completion above).
        items = removeDuplicateBorders(items);
        // 9. Sort into z-order (rects under lines under images under text) for correct paint order.
        items = sortByZOrder(items);

        LayoutSpec processed = new LayoutSpec();
        processed.setItems(items);
        processed.setPas(input.getPas());
        return processed;
    }

    private List<LayoutItem> enforceConsistency(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            if (isText(item)) {
                item.setTransparent(true);
                if (item.getFontSize() == null) {
                    item.setFontSize(9);
                }
            }
            if (item.getFontSize() != null) {
                item.setFontSize(Math.clamp(item.getFontSize(), MIN_FONT_SIZE, MAX_FONT_SIZE));
            }
            if (item.getThickness() != null) {
                item.setThickness(Math.clamp(item.getThickness(), 1, MAX_LINE_THICKNESS));
            }
            if (isLine(item) && (item.getStrokeColor() == null || item.getStrokeColor().isBlank())) {
                item.setStrokeColor(TableBorderCompleter.DEFAULT_BORDER_COLOR);
            }
            item.setLeft(Math.max(0, item.getLeft()));
            item.setTop(Math.max(0, item.getTop()));
            item.setWidth(Math.max(1, item.getWidth()));
            item.setHeight(Math.max(1, item.getHeight()));
        }
        return items;
    }

    private List<LayoutItem> enforceCanvasBounds(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            item.setLeft(Math.clamp(item.getLeft(), CANVAS_LEFT, CANVAS_RIGHT - 1));
            item.setTop(Math.clamp(item.getTop(), CANVAS_TOP, CANVAS_BOTTOM - 1));
            item.setWidth(Math.max(1, Math.min(item.getWidth(), CANVAS_RIGHT - item.getLeft())));
            item.setHeight(Math.max(1, Math.min(item.getHeight(), CANVAS_BOTTOM - item.getTop())));
            if (isHorizontalLine(item)) {
                item.setHeight(1);
            } else if (isVerticalLine(item)) {
                item.setWidth(1);
            }
        }
        return items;
    }

    private List<LayoutItem> removeDuplicateBorders(List<LayoutItem> items) {
        Set<String> seen = new HashSet<>();
        List<LayoutItem> deduped = new ArrayList<>();
        for (LayoutItem item : items) {
            if (!isLine(item)) {
                deduped.add(item);
                continue;
            }
            String key = normalizeOrientation(item) + "|" + item.getLeft() + "|" + item.getTop() + "|" + item.getWidth() + "|" + item.getHeight();
            if (seen.add(key)) {
                deduped.add(item);
            }
        }
        return deduped;
    }

    private List<LayoutItem> sortByZOrder(List<LayoutItem> items) {
        return items.stream()
            .sorted(Comparator.<LayoutItem>comparingInt(item -> zPriority(item.getType()))
                .thenComparingInt(LayoutItem::getTop)
                .thenComparingInt(LayoutItem::getLeft))
            .toList();
    }

    private int zPriority(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        return switch (normalized) {
            case "rect" -> 0;
            case "line" -> 1;
            case "image" -> 2;
            case "text" -> 3;
            default -> 4;
        };
    }
}
