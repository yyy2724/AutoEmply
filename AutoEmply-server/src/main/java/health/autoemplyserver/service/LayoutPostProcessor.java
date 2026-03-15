package health.autoemplyserver.service;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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
        List<LayoutItem> items = new ArrayList<>();
        if (input.getItems() != null) {
            input.getItems().forEach(item -> items.add(item.copy()));
        }

        snapToGrid(items);
        alignEdges(items);
        normalizeRowHeights(items);
        completeBorders(items);
        normalizeLineGrid(items);
        enforceConsistency(items);
        enforceCanvasBounds(items);
        removeDuplicateBorders(items);
        sortByZOrder(items);

        LayoutSpec output = new LayoutSpec();
        output.setItems(items);
        output.setPas(input.getPas());
        return output;
    }

    private void snapToGrid(List<LayoutItem> items) {
        Map<Integer, Integer> xFreq = new HashMap<>();
        Map<Integer, Integer> yFreq = new HashMap<>();
        for (LayoutItem item : items) {
            increment(xFreq, item.getLeft());
            increment(xFreq, item.getLeft() + item.getWidth());
            increment(yFreq, item.getTop());
            increment(yFreq, item.getTop() + item.getHeight());
        }
        List<Integer> xGrid = xFreq.entrySet().stream().filter(entry -> entry.getValue() >= 2).map(Map.Entry::getKey).sorted().toList();
        List<Integer> yGrid = yFreq.entrySet().stream().filter(entry -> entry.getValue() >= 2).map(Map.Entry::getKey).sorted().toList();

        for (LayoutItem item : items) {
            int originalLeft = item.getLeft();
            item.setLeft(snapToNearest(item.getLeft(), xGrid, SNAP_THRESHOLD));
            item.setWidth(Math.max(1, snapToNearest(originalLeft + item.getWidth(), xGrid, SNAP_THRESHOLD) - item.getLeft()));
            int originalTop = item.getTop();
            item.setTop(snapToNearest(item.getTop(), yGrid, SNAP_THRESHOLD));
            item.setHeight(Math.max(1, snapToNearest(originalTop + item.getHeight(), yGrid, SNAP_THRESHOLD) - item.getTop()));
        }
    }

    private void alignEdges(List<LayoutItem> items) {
        alignCoordinate(items, LayoutItem::getTop, LayoutItem::setTop);
        alignCoordinate(items, LayoutItem::getLeft, LayoutItem::setLeft);
        alignCoordinate(items, item -> item.getLeft() + item.getWidth(), (item, value) -> item.setWidth(Math.max(1, value - item.getLeft())));
        alignCoordinate(items, item -> item.getTop() + item.getHeight(), (item, value) -> item.setHeight(Math.max(1, value - item.getTop())));
    }

    private void normalizeRowHeights(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            if (isText(item) && item.getHeight() >= 11 && item.getHeight() <= 15) {
                item.setHeight(STANDARD_LABEL_HEIGHT);
            }
            if (isHLine(item)) {
                item.setHeight(1);
            }
            if (isVLine(item)) {
                item.setWidth(1);
            }
        }
    }

    private void completeBorders(List<LayoutItem> items) {
        List<LayoutItem> wideHLines = items.stream()
            .filter(this::isHLine)
            .filter(item -> item.getWidth() > CONTENT_WIDTH * 0.4)
            .sorted(Comparator.comparingInt(LayoutItem::getTop))
            .toList();
        if (wideHLines.size() < 2) {
            return;
        }

        List<int[]> regions = detectTableRegions(wideHLines);
        for (int[] region : regions) {
            int top = region[0];
            int bottom = region[1];
            int left = region[2];
            int right = region[3];
            int height = bottom - top;
            if (height <= 0) {
                continue;
            }
            List<LayoutItem> vLines = items.stream()
                .filter(this::isVLine)
                .filter(item -> item.getTop() >= top - 4 && item.getTop() <= bottom + 4)
                .filter(item -> item.getLeft() >= left - 4 && item.getLeft() <= right + 4)
                .toList();
            for (LayoutItem line : vLines) {
                boolean nearTop = Math.abs(line.getTop() - top) <= 6;
                boolean nearBottom = Math.abs(line.getTop() + line.getHeight() - bottom) <= 6;
                if ((nearTop || nearBottom) && line.getHeight() >= (int) (height * 0.6) && line.getHeight() < height - 4) {
                    line.setTop(top);
                    line.setHeight(height);
                }
            }

            Set<Integer> existingX = new HashSet<>();
            vLines.forEach(line -> existingX.add(line.getLeft()));
            if (existingX.stream().noneMatch(x -> Math.abs(x - left) <= 3)) {
                items.add(makeBorderVLine(left, top, height));
            }
            if (existingX.stream().noneMatch(x -> Math.abs(x - right) <= 3)) {
                items.add(makeBorderVLine(right, top, height));
            }
        }
    }

    private List<int[]> detectTableRegions(List<LayoutItem> lines) {
        List<int[]> regions = new ArrayList<>();
        LayoutItem start = lines.getFirst();
        int regionBottom = start.getTop();
        int regionLeft = start.getLeft();
        int regionRight = start.getLeft() + start.getWidth();

        for (int index = 1; index < lines.size(); index++) {
            LayoutItem line = lines.get(index);
            if (line.getTop() - regionBottom > 24) {
                if (regionBottom > start.getTop()) {
                    regions.add(new int[] {start.getTop(), regionBottom, regionLeft, regionRight});
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
            regions.add(new int[] {start.getTop(), regionBottom, regionLeft, regionRight});
        }
        return regions;
    }

    private void normalizeLineGrid(List<LayoutItem> items) {
        List<LayoutItem> vLines = items.stream().filter(this::isVLine).toList();
        List<LayoutItem> hLines = items.stream().filter(this::isHLine).toList();
        if (vLines.isEmpty() || hLines.isEmpty()) {
            return;
        }
        List<Integer> xGrid = vLines.stream().map(LayoutItem::getLeft).distinct().sorted().toList();
        List<Integer> yGrid = hLines.stream().map(LayoutItem::getTop).distinct().sorted().toList();

        for (LayoutItem line : hLines) {
            line.setTop(snapToNearest(line.getTop(), yGrid, 4));
            int right = line.getLeft() + line.getWidth();
            line.setLeft(snapToNearest(line.getLeft(), xGrid, 8));
            line.setWidth(Math.max(1, snapToNearest(right, xGrid, 8) - line.getLeft()));
            line.setHeight(1);
            line.setOrientation("H");
        }

        for (LayoutItem line : vLines) {
            line.setLeft(snapToNearest(line.getLeft(), xGrid, 4));
            int bottom = line.getTop() + line.getHeight();
            line.setTop(snapToNearest(line.getTop(), yGrid, 8));
            line.setHeight(Math.max(1, snapToNearest(bottom, yGrid, 8) - line.getTop()));
            line.setWidth(1);
            line.setOrientation("V");
        }
    }

    private void enforceConsistency(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            if (isText(item)) {
                item.setTransparent(true);
                if (item.getFontSize() == null) {
                    item.setFontSize(9);
                }
            }
            if (item.getFontSize() != null) {
                item.setFontSize(Math.max(6, Math.min(24, item.getFontSize())));
            }
            if (item.getThickness() != null) {
                item.setThickness(Math.max(1, Math.min(6, item.getThickness())));
            }
            if (isLine(item) && (item.getStrokeColor() == null || item.getStrokeColor().isBlank())) {
                item.setStrokeColor(DEFAULT_BORDER_COLOR);
            }
            item.setLeft(Math.max(0, item.getLeft()));
            item.setTop(Math.max(0, item.getTop()));
            item.setWidth(Math.max(1, item.getWidth()));
            item.setHeight(Math.max(1, item.getHeight()));
        }
    }

    private void enforceCanvasBounds(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            item.setLeft(Math.max(CANVAS_LEFT, Math.min(CANVAS_RIGHT - 1, item.getLeft())));
            item.setTop(Math.max(CANVAS_TOP, Math.min(CANVAS_BOTTOM - 1, item.getTop())));
            item.setWidth(Math.max(1, Math.min(item.getWidth(), CANVAS_RIGHT - item.getLeft())));
            item.setHeight(Math.max(1, Math.min(item.getHeight(), CANVAS_BOTTOM - item.getTop())));
            if (isHLine(item)) {
                item.setHeight(1);
            } else if (isVLine(item)) {
                item.setWidth(1);
            }
        }
    }

    private void removeDuplicateBorders(List<LayoutItem> items) {
        Set<String> chosen = new HashSet<>();
        List<LayoutItem> deduped = new ArrayList<>();
        for (LayoutItem item : items) {
            if (!isLine(item)) {
                deduped.add(item);
                continue;
            }
            String key = normalizeOrientation(item) + "|" + item.getLeft() + "|" + item.getTop() + "|" + item.getWidth() + "|" + item.getHeight();
            if (chosen.add(key)) {
                deduped.add(item);
            }
        }
        items.clear();
        items.addAll(deduped);
    }

    private void sortByZOrder(List<LayoutItem> items) {
        items.sort(
            Comparator.<LayoutItem>comparingInt(item -> zPriority(item.getType()))
                .thenComparingInt(LayoutItem::getTop)
                .thenComparingInt(LayoutItem::getLeft)
        );
    }

    private int zPriority(String type) {
        String normalized = type == null ? "" : type.toLowerCase();
        return switch (normalized) {
            case "rect" -> 0;
            case "line" -> 1;
            case "image" -> 2;
            case "text" -> 3;
            default -> 4;
        };
    }

    private void alignCoordinate(List<LayoutItem> items, ToIntFunction<LayoutItem> getter, BiConsumer<LayoutItem, Integer> setter) {
        List<LayoutItem> sorted = items.stream().sorted(Comparator.comparingInt(getter)).toList();
        List<List<LayoutItem>> groups = new ArrayList<>();
        if (sorted.isEmpty()) {
            return;
        }
        List<LayoutItem> current = new ArrayList<>();
        current.add(sorted.getFirst());
        for (int index = 1; index < sorted.size(); index++) {
            if (Math.abs(getter.applyAsInt(sorted.get(index)) - getter.applyAsInt(sorted.get(index - 1))) <= ALIGN_THRESHOLD) {
                current.add(sorted.get(index));
            } else {
                groups.add(current);
                current = new ArrayList<>();
                current.add(sorted.get(index));
            }
        }
        groups.add(current);

        for (List<LayoutItem> group : groups) {
            if (group.size() < 2) {
                continue;
            }
            Map<Integer, Integer> frequency = new HashMap<>();
            group.forEach(item -> increment(frequency, getter.applyAsInt(item)));
            int canonical = frequency.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(getter.applyAsInt(group.getFirst()));
            group.forEach(item -> setter.accept(item, canonical));
        }
    }

    private int snapToNearest(int value, List<Integer> grid, int threshold) {
        if (grid.isEmpty()) {
            return value;
        }
        int closest = value;
        int minDistance = Integer.MAX_VALUE;
        for (int point : grid) {
            int distance = Math.abs(value - point);
            if (distance < minDistance) {
                minDistance = distance;
                closest = point;
            }
            if (distance == 0) {
                break;
            }
            if (point > value + threshold) {
                break;
            }
        }
        return minDistance <= threshold ? closest : value;
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

    private boolean isText(LayoutItem item) {
        return "Text".equalsIgnoreCase(item.getType());
    }

    private boolean isLine(LayoutItem item) {
        return "Line".equalsIgnoreCase(item.getType());
    }

    private boolean isHLine(LayoutItem item) {
        return isLine(item) && ("H".equalsIgnoreCase(item.getOrientation()) || item.getWidth() > item.getHeight());
    }

    private boolean isVLine(LayoutItem item) {
        return isLine(item) && ("V".equalsIgnoreCase(item.getOrientation()) || item.getHeight() > item.getWidth());
    }

    private String normalizeOrientation(LayoutItem item) {
        if (isHLine(item)) {
            return "H";
        }
        if (isVLine(item)) {
            return "V";
        }
        return item.getOrientation() == null ? "" : item.getOrientation().trim().toUpperCase();
    }
}
