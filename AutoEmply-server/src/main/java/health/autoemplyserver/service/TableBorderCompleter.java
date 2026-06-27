package health.autoemplyserver.service;

import static health.autoemplyserver.service.LayoutItemKinds.isHorizontalLine;
import static health.autoemplyserver.service.LayoutItemKinds.isVerticalLine;

import health.autoemplyserver.model.LayoutItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Single responsibility: table border completion — detects table regions from wide
 * horizontal lines, stretches near-full-height vertical lines to the region height, and
 * adds missing left/right border lines. Owns all region-detection tolerances. Stateless;
 * orchestrated by {@link LayoutPostProcessor}.
 */
class TableBorderCompleter {

    /** Default stroke color for border lines created or normalized during post-processing. */
    static final String DEFAULT_BORDER_COLOR = "#000000";

    // Usable content width in px (canvas right edge minus left edge); see the canvas
    // constants in LayoutPostProcessor.
    private static final int CONTENT_WIDTH = 774;

    // Table border completion (pixels / ratios).
    private static final double WIDE_LINE_WIDTH_RATIO = 0.4;     // horizontal line counts as a table row separator if wider than this fraction of CONTENT_WIDTH
    private static final int TABLE_REGION_MAX_ROW_GAP = 24;      // px; a vertical gap larger than this between horizontal lines starts a new table region
    private static final int REGION_ATTACH_TOLERANCE = 4;        // px tolerance when testing whether a line lies within / spans a table region
    private static final int LINE_END_PROXIMITY = 6;             // px; a vertical line endpoint this close to the region top/bottom counts as touching it
    private static final double FULL_HEIGHT_EXTEND_RATIO = 0.6;  // a vertical line covering at least this fraction of the region height is stretched to full height
    private static final int BORDER_X_MATCH_TOLERANCE = 3;       // px; an existing vertical line this close to the region edge counts as its border

    List<LayoutItem> completeBorders(List<LayoutItem> items) {
        List<LayoutItem> wideHLines = items.stream()
            .filter(item -> isHorizontalLine(item) && item.getWidth() > CONTENT_WIDTH * WIDE_LINE_WIDTH_RATIO)
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
                    && item.getTop() >= region.top() - REGION_ATTACH_TOLERANCE && item.getTop() <= region.bottom() + REGION_ATTACH_TOLERANCE
                    && item.getLeft() >= region.left() - REGION_ATTACH_TOLERANCE && item.getLeft() <= region.right() + REGION_ATTACH_TOLERANCE)
                .toList();

            Set<Integer> existingXs = new HashSet<>();
            for (LayoutItem line : verticals) {
                existingXs.add(line.getLeft());
                boolean nearTop = Math.abs(line.getTop() - region.top()) <= LINE_END_PROXIMITY;
                boolean nearBottom = Math.abs(line.getTop() + line.getHeight() - region.bottom()) <= LINE_END_PROXIMITY;
                if ((nearTop || nearBottom) && line.getHeight() >= (int) (height * FULL_HEIGHT_EXTEND_RATIO) && line.getHeight() < height - REGION_ATTACH_TOLERANCE) {
                    line.setTop(region.top());
                    line.setHeight(height);
                }
            }

            if (existingXs.stream().noneMatch(x -> Math.abs(x - region.left()) <= BORDER_X_MATCH_TOLERANCE)) {
                items.add(makeBorderVLine(region.left(), region.top(), height));
            }
            if (existingXs.stream().noneMatch(x -> Math.abs(x - region.right()) <= BORDER_X_MATCH_TOLERANCE)) {
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
            if (line.getTop() - regionBottom > TABLE_REGION_MAX_ROW_GAP) {
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

    private record TableRegion(int top, int bottom, int left, int right) {
    }
}
