package health.autoemplyserver.service;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Component;

@Component
public class LayoutPostProcessor {

    private static final int CONTENT_WIDTH = 774;
    private static final int CANVAS_LEFT = 10;
    private static final int CANVAS_RIGHT = 784;
    private static final int CANVAS_TOP = 0;
    private static final int CANVAS_BOTTOM = 1600;
    private static final int SNAP_THRESHOLD = 4;
    private static final int ALIGN_THRESHOLD = 3;
    private static final int STANDARD_LABEL_HEIGHT = 13;
    private static final String DEFAULT_BORDER_COLOR = "#000000";

    public LayoutSpec process(LayoutSpec input) {
        if (input == null) {
            return null;
        }

        List<LayoutItem> items = new ArrayList<>();
        for (LayoutItem item : input.getItems() == null ? List.<LayoutItem>of() : input.getItems()) {
            items.add(item == null ? new LayoutItem() : item.copy());
        }

        items = snapToGrid(items);
        items = alignEdges(items);
        items = normalizeRowHeights(items);
        items = completeBorders(items);
        items = normalizeLineGrid(items);
        items = enforceConsistency(items);
        items = enforceCanvasBounds(items);
        items = removeDuplicateBorders(items);
        items = sortByZOrder(items);

        LayoutSpec processed = new LayoutSpec();
        processed.setItems(items);
        processed.setPas(input.getPas());
        return processed;
    }

    private List<LayoutItem> snapToGrid(List<LayoutItem> items) {
        Map<Integer, Integer> xFreq = new HashMap<>();
        Map<Integer, Integer> yFreq = new HashMap<>();

        for (LayoutItem item : items) {
            increment(xFreq, item.getLeft());
            increment(xFreq, item.getLeft() + item.getWidth());
            increment(yFreq, item.getTop());
            increment(yFreq, item.getTop() + item.getHeight());
        }

        List<Integer> xGrid = xFreq.entrySet().stream()
            .filter(entry -> entry.getValue() >= 2)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        List<Integer> yGrid = yFreq.entrySet().stream()
            .filter(entry -> entry.getValue() >= 2)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

        for (LayoutItem item : items) {
            int originalLeft = item.getLeft();
            int originalTop = item.getTop();
            item.setLeft(snapToNearest(item.getLeft(), xGrid, SNAP_THRESHOLD));
            item.setWidth(Math.max(1, snapToNearest(originalLeft + item.getWidth(), xGrid, SNAP_THRESHOLD) - item.getLeft()));
            item.setTop(snapToNearest(item.getTop(), yGrid, SNAP_THRESHOLD));
            item.setHeight(Math.max(1, snapToNearest(originalTop + item.getHeight(), yGrid, SNAP_THRESHOLD) - item.getTop()));
        }
        return items;
    }

    private List<LayoutItem> alignEdges(List<LayoutItem> items) {
        alignCoordinate(items, LayoutItem::getTop, LayoutItem::setTop, ALIGN_THRESHOLD);
        alignCoordinate(items, LayoutItem::getLeft, LayoutItem::setLeft, ALIGN_THRESHOLD);
        alignCoordinate(items, item -> item.getLeft() + item.getWidth(), (item, value) -> item.setWidth(Math.max(1, value - item.getLeft())), ALIGN_THRESHOLD);
        alignCoordinate(items, item -> item.getTop() + item.getHeight(), (item, value) -> item.setHeight(Math.max(1, value - item.getTop())), ALIGN_THRESHOLD);
        return items;
    }

    private List<LayoutItem> normalizeRowHeights(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            if (isText(item) && item.getHeight() >= 11 && item.getHeight() <= 15) {
                item.setHeight(STANDARD_LABEL_HEIGHT);
            }
            if (isHorizontalLine(item)) {
                item.setHeight(1);
            }
            if (isVerticalLine(item)) {
                item.setWidth(1);
            }
        }
        return items;
    }

    private List<LayoutItem> completeBorders(List<LayoutItem> items) {
        List<LayoutItem> wideHLines = items.stream()
            .filter(item -> isHorizontalLine(item) && item.getWidth() > CONTENT_WIDTH * 0.4)
            .sorted(Comparator.comparingInt(LayoutItem::getTop))
            .toList();
        if (wideHLines.size() < 2) {
            return items;
        }

        for (TableRegion region : detectTableRegions(wideHLines)) {
            int height = region.bottom() - region.top();
            if (height <= 0) {
                continue;
            }

            List<LayoutItem> verticals = items.stream()
                .filter(item -> isVerticalLine(item)
                    && item.getTop() >= region.top() - 4 && item.getTop() <= region.bottom() + 4
                    && item.getLeft() >= region.left() - 4 && item.getLeft() <= region.right() + 4)
                .toList();

            Set<Integer> existingXs = new HashSet<>();
            for (LayoutItem line : verticals) {
                existingXs.add(line.getLeft());
                boolean nearTop = Math.abs(line.getTop() - region.top()) <= 6;
                boolean nearBottom = Math.abs(line.getTop() + line.getHeight() - region.bottom()) <= 6;
                if ((nearTop || nearBottom) && line.getHeight() >= (int) (height * 0.6) && line.getHeight() < height - 4) {
                    line.setTop(region.top());
                    line.setHeight(height);
                }
            }

            if (existingXs.stream().noneMatch(x -> Math.abs(x - region.left()) <= 3)) {
                items.add(makeBorderVLine(region.left(), region.top(), height));
            }
            if (existingXs.stream().noneMatch(x -> Math.abs(x - region.right()) <= 3)) {
                items.add(makeBorderVLine(region.right(), region.top(), height));
            }
        }
        return items;
    }

    private List<TableRegion> detectTableRegions(List<LayoutItem> sortedHLines) {
        List<TableRegion> regions = new ArrayList<>();
        LayoutItem start = sortedHLines.getFirst();
        int regionBottom = start.getTop();
        int regionLeft = start.getLeft();
        int regionRight = start.getLeft() + start.getWidth();

        for (int index = 1; index < sortedHLines.size(); index++) {
            LayoutItem line = sortedHLines.get(index);
            if (line.getTop() - regionBottom > 24) {
                if (regionBottom > start.getTop()) {
                    regions.add(new TableRegion(start.getTop(), regionBottom, regionLeft, regionRight));
                }
                start = line;
                regionBottom = line.getTop();
                regionLeft = line.getLeft();
                regionRight = line.getLeft() + line.getWidth();
            } else {
                regionBottom = line.getTop();
                regionLeft = Math.min(regionLeft, line.getLeft());
                regionRight = Math.max(regionRight, line.getLeft() + line.getWidth());
            }
        }

        if (regionBottom > start.getTop()) {
            regions.add(new TableRegion(start.getTop(), regionBottom, regionLeft, regionRight));
        }
        return regions;
    }

    private List<LayoutItem> normalizeLineGrid(List<LayoutItem> items) {
        List<LayoutItem> verticals = items.stream().filter(this::isVerticalLine).toList();
        List<LayoutItem> horizontals = items.stream().filter(this::isHorizontalLine).toList();
        if (verticals.isEmpty() || horizontals.isEmpty()) {
            return items;
        }

        List<Integer> xGrid = verticals.stream().map(LayoutItem::getLeft).distinct().sorted().toList();
        List<Integer> yGrid = horizontals.stream().map(LayoutItem::getTop).distinct().sorted().toList();

        for (LayoutItem line : horizontals) {
            int right = line.getLeft() + line.getWidth();
            line.setTop(snapToNearest(line.getTop(), yGrid, 4));
            line.setLeft(snapToNearest(line.getLeft(), xGrid, 8));
            line.setWidth(Math.max(1, snapToNearest(right, xGrid, 8) - line.getLeft()));
            line.setHeight(1);
            line.setOrientation("H");
        }

        for (LayoutItem line : verticals) {
            int bottom = line.getTop() + line.getHeight();
            line.setLeft(snapToNearest(line.getLeft(), xGrid, 4));
            line.setTop(snapToNearest(line.getTop(), yGrid, 8));
            line.setHeight(Math.max(1, snapToNearest(bottom, yGrid, 8) - line.getTop()));
            line.setWidth(1);
            line.setOrientation("V");
        }
        return items;
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
                item.setFontSize(Math.clamp(item.getFontSize(), 6, 24));
            }
            if (item.getThickness() != null) {
                item.setThickness(Math.clamp(item.getThickness(), 1, 6));
            }
            if (isLine(item) && (item.getStrokeColor() == null || item.getStrokeColor().isBlank())) {
                item.setStrokeColor(DEFAULT_BORDER_COLOR);
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

    private void alignCoordinate(List<LayoutItem> items, ToIntFunction<LayoutItem> getter, ObjIntConsumer<LayoutItem> setter, int threshold) {
        List<List<LayoutItem>> groups = groupByProximity(items, getter, threshold);
        for (List<LayoutItem> group : groups) {
            if (group.size() < 2) {
                continue;
            }
            Map<Integer, Integer> frequency = new HashMap<>();
            for (LayoutItem item : group) {
                increment(frequency, getter.applyAsInt(item));
            }
            int canonical = frequency.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue).thenComparingInt(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse(getter.applyAsInt(group.getFirst()));
            for (LayoutItem item : group) {
                setter.accept(item, canonical);
            }
        }
    }

    private List<List<LayoutItem>> groupByProximity(List<LayoutItem> items, ToIntFunction<LayoutItem> getter, int threshold) {
        List<LayoutItem> sorted = items.stream().sorted(Comparator.comparingInt(getter)).toList();
        List<List<LayoutItem>> groups = new ArrayList<>();
        if (sorted.isEmpty()) {
            return groups;
        }

        List<LayoutItem> current = new ArrayList<>();
        current.add(sorted.getFirst());
        for (int index = 1; index < sorted.size(); index++) {
            if (Math.abs(getter.applyAsInt(sorted.get(index)) - getter.applyAsInt(sorted.get(index - 1))) <= threshold) {
                current.add(sorted.get(index));
            } else {
                groups.add(current);
                current = new ArrayList<>();
                current.add(sorted.get(index));
            }
        }
        groups.add(current);
        return groups;
    }

    private int snapToNearest(int value, List<Integer> grid, int threshold) {
        if (grid.isEmpty()) {
            return value;
        }
        int closest = value;
        int minDist = Integer.MAX_VALUE;
        for (int candidate : grid) {
            int distance = Math.abs(value - candidate);
            if (distance < minDist) {
                minDist = distance;
                closest = candidate;
            }
            if (distance == 0 || candidate > value + threshold) {
                break;
            }
        }
        return minDist <= threshold ? closest : value;
    }

    private void increment(Map<Integer, Integer> frequency, int key) {
        frequency.put(key, frequency.getOrDefault(key, 0) + 1);
    }

    private LayoutItem makeBorderVLine(int left, int top, int height) {
        LayoutItem item = new LayoutItem();
        item.setType("Line");
        item.setLeft(left);
        item.setTop(top);
        item.setWidth(1);
        item.setHeight(height);
        item.setOrientation("V");
        item.setThickness(1);
        item.setStrokeColor(DEFAULT_BORDER_COLOR);
        return item;
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

    private boolean isText(LayoutItem item) {
        return "text".equalsIgnoreCase(item.getType());
    }

    private boolean isLine(LayoutItem item) {
        return "line".equalsIgnoreCase(item.getType());
    }

    private boolean isHorizontalLine(LayoutItem item) {
        return isLine(item) && ("H".equalsIgnoreCase(item.getOrientation()) || item.getWidth() > item.getHeight());
    }

    private boolean isVerticalLine(LayoutItem item) {
        return isLine(item) && ("V".equalsIgnoreCase(item.getOrientation()) || item.getHeight() > item.getWidth());
    }

    private String normalizeOrientation(LayoutItem item) {
        if (isHorizontalLine(item)) {
            return "H";
        }
        if (isVerticalLine(item)) {
            return "V";
        }
        return Objects.toString(item.getOrientation(), "").trim().toUpperCase();
    }

    private record TableRegion(int top, int bottom, int left, int right) {
    }
}
