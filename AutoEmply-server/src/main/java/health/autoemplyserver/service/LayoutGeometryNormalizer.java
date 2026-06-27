package health.autoemplyserver.service;

import static health.autoemplyserver.service.LayoutItemKinds.isHorizontalLine;
import static health.autoemplyserver.service.LayoutItemKinds.isText;
import static health.autoemplyserver.service.LayoutItemKinds.isVerticalLine;

import health.autoemplyserver.model.LayoutItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * Single responsibility: coordinate cleanup of AI-generated layout items — snapping edges
 * onto the dominant x/y grid, merging near-identical edge coordinates, normalizing text
 * label heights and line thickness, and snapping line endpoints onto the crossing-line
 * grid. Owns all snap/align tolerances. Stateless; orchestrated by
 * {@link LayoutPostProcessor}.
 */
class LayoutGeometryNormalizer {

    // Coordinate cleanup tolerances (pixels).
    private static final int SNAP_THRESHOLD = 4;    // max px distance for snapping an edge onto the inferred grid
    private static final int ALIGN_THRESHOLD = 3;   // max px distance for merging near-identical edge coordinates
    private static final int STANDARD_LABEL_HEIGHT = 13;  // canonical single-line label height in px
    private static final int LABEL_HEIGHT_SNAP_MIN = 11;  // px; text heights in [MIN, MAX] are normalized to STANDARD_LABEL_HEIGHT
    private static final int LABEL_HEIGHT_SNAP_MAX = 15;

    // Line grid normalization (pixels).
    private static final int LINE_AXIS_SNAP_THRESHOLD = 4;      // px; snap distance for a line's own axis position (y of an H line, x of a V line)
    private static final int LINE_ENDPOINT_SNAP_THRESHOLD = 8;  // px; snap distance for line endpoints onto the crossing-line grid

    List<LayoutItem> snapToGrid(List<LayoutItem> items) {
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

    List<LayoutItem> alignEdges(List<LayoutItem> items) {
        alignCoordinate(items, LayoutItem::getTop, LayoutItem::setTop, ALIGN_THRESHOLD);
        alignCoordinate(items, LayoutItem::getLeft, LayoutItem::setLeft, ALIGN_THRESHOLD);
        alignCoordinate(items, item -> item.getLeft() + item.getWidth(), (item, value) -> item.setWidth(Math.max(1, value - item.getLeft())), ALIGN_THRESHOLD);
        alignCoordinate(items, item -> item.getTop() + item.getHeight(), (item, value) -> item.setHeight(Math.max(1, value - item.getTop())), ALIGN_THRESHOLD);
        return items;
    }

    List<LayoutItem> normalizeTextAndLineSizes(List<LayoutItem> items) {
        for (LayoutItem item : items) {
            if (isText(item) && item.getHeight() >= LABEL_HEIGHT_SNAP_MIN && item.getHeight() <= LABEL_HEIGHT_SNAP_MAX) {
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

    List<LayoutItem> normalizeLineGrid(List<LayoutItem> items) {
        List<LayoutItem> verticals = items.stream().filter(LayoutItemKinds::isVerticalLine).toList();
        List<LayoutItem> horizontals = items.stream().filter(LayoutItemKinds::isHorizontalLine).toList();
        if (verticals.isEmpty() || horizontals.isEmpty()) {
            return items;
        }

        List<Integer> xGrid = verticals.stream().map(LayoutItem::getLeft).distinct().sorted().toList();
        List<Integer> yGrid = horizontals.stream().map(LayoutItem::getTop).distinct().sorted().toList();

        for (LayoutItem line : horizontals) {
            int right = line.getLeft() + line.getWidth();
            line.setTop(snapToNearest(line.getTop(), yGrid, LINE_AXIS_SNAP_THRESHOLD));
            line.setLeft(snapToNearest(line.getLeft(), xGrid, LINE_ENDPOINT_SNAP_THRESHOLD));
            line.setWidth(Math.max(1, snapToNearest(right, xGrid, LINE_ENDPOINT_SNAP_THRESHOLD) - line.getLeft()));
            line.setHeight(1);
            line.setOrientation("H");
        }

        for (LayoutItem line : verticals) {
            int bottom = line.getTop() + line.getHeight();
            line.setLeft(snapToNearest(line.getLeft(), xGrid, LINE_AXIS_SNAP_THRESHOLD));
            line.setTop(snapToNearest(line.getTop(), yGrid, LINE_ENDPOINT_SNAP_THRESHOLD));
            line.setHeight(Math.max(1, snapToNearest(bottom, yGrid, LINE_ENDPOINT_SNAP_THRESHOLD) - line.getTop()));
            line.setWidth(1);
            line.setOrientation("V");
        }
        return items;
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
}
