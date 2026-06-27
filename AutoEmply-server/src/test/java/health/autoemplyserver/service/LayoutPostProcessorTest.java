package health.autoemplyserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.model.PasSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutPostProcessorTest {

    private final LayoutPostProcessor processor = new LayoutPostProcessor();

    @Test
    void nullSpecReturnsNull() {
        assertThat(processor.process(null)).isNull();
    }

    @Test
    void snapsEdgesOntoDominantGridWithinSnapThreshold() {
        // left = 100 appears twice, so it forms the x grid; 103 is 3px away (within threshold 4).
        LayoutSpec result = processor.process(spec(
            text("A", 100, 200, 50, 20),
            text("B", 100, 300, 50, 20),
            text("C", 103, 400, 47, 20)));

        LayoutItem snapped = byName(result, "C");
        assertThat(snapped.getLeft()).isEqualTo(100);
        assertThat(snapped.getWidth()).isEqualTo(50); // right edge 150 stays on the shared grid
    }

    @Test
    void doesNotSnapEdgesBeyondSnapThreshold() {
        // 105 is 5px away from the grid value 100, just beyond the threshold of 4.
        LayoutSpec result = processor.process(spec(
            text("A", 100, 200, 50, 20),
            text("B", 100, 300, 50, 20),
            text("C", 105, 400, 45, 20)));

        assertThat(byName(result, "C").getLeft()).isEqualTo(105);
    }

    @Test
    void mergesNearIdenticalTopEdgesWithinAlignThreshold() {
        // Tops 200/202/203 are within 3px of each other and collapse onto one canonical value.
        LayoutSpec result = processor.process(spec(
            text("A", 100, 200, 50, 20),
            text("B", 300, 202, 50, 20),
            text("C", 500, 203, 50, 20)));

        assertThat(result.getItems()).extracting(LayoutItem::getTop).containsOnly(203);
    }

    @Test
    void keepsTopEdgesApartBeyondAlignThreshold() {
        // A gap of 4px exceeds the align threshold of 3, so both rows stay where they are.
        LayoutSpec result = processor.process(spec(
            text("A", 100, 200, 50, 20),
            text("B", 300, 204, 50, 20)));

        assertThat(result.getItems()).extracting(LayoutItem::getTop)
            .containsExactlyInAnyOrder(200, 204);
    }

    @Test
    void normalizesNearStandardTextHeightsToStandardLabelHeight() {
        // Text heights in [11, 15] snap to the canonical 13px label height; others stay.
        LayoutSpec result = processor.process(spec(
            text("A", 100, 100, 50, 11),
            text("B", 300, 300, 50, 12),
            text("C", 500, 500, 50, 15),
            text("D", 700, 700, 50, 20)));

        assertThat(byName(result, "A").getHeight()).isEqualTo(13);
        assertThat(byName(result, "B").getHeight()).isEqualTo(13);
        assertThat(byName(result, "C").getHeight()).isEqualTo(13);
        assertThat(byName(result, "D").getHeight()).isEqualTo(20);
    }

    @Test
    void removesDuplicateBorderLinesWithSameGeometry() {
        LayoutSpec result = processor.process(spec(
            line("H", 10, 50, 300, 1),
            line("H", 10, 50, 300, 1),
            line("H", 10, 400, 300, 1)));

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems()).extracting(LayoutItem::getTop)
            .containsExactlyInAnyOrder(50, 400);
    }

    @Test
    void clampsItemsIntoDrawableCanvasBounds() {
        LayoutSpec result = processor.process(spec(
            rect("LEFT_OVERFLOW", -5, 100, 50, 30),
            rect("RIGHT_OVERFLOW", 800, 300, 50, 30),
            rect("BOTTOM_OVERFLOW", 100, 1700, 50, 30)));

        LayoutItem leftOverflow = byName(result, "LEFT_OVERFLOW");
        assertThat(leftOverflow.getLeft()).isEqualTo(10);   // canvas left edge
        assertThat(leftOverflow.getWidth()).isEqualTo(50);

        LayoutItem rightOverflow = byName(result, "RIGHT_OVERFLOW");
        assertThat(rightOverflow.getLeft()).isEqualTo(783); // canvas right edge - 1
        assertThat(rightOverflow.getWidth()).isEqualTo(1);

        LayoutItem bottomOverflow = byName(result, "BOTTOM_OVERFLOW");
        assertThat(bottomOverflow.getTop()).isEqualTo(1599); // canvas bottom edge - 1
        assertThat(bottomOverflow.getHeight()).isEqualTo(1);
    }

    @Test
    void sortsRectsUnderLinesUnderImagesUnderText() {
        LayoutItem image = item("Image", 300, 300, 80, 40);
        LayoutItem rectItem = item("Rect", 400, 400, 60, 30);

        LayoutSpec result = processor.process(spec(
            text("T", 100, 100, 50, 13),
            image,
            line("H", 200, 200, 100, 1),
            rectItem));

        assertThat(result.getItems()).extracting(LayoutItem::getType)
            .containsExactly("Rect", "Line", "Image", "Text");
    }

    @Test
    void addsMissingVerticalBordersAroundDetectedTableRegion() {
        // Two wide horizontal lines 20px apart form a table region without side borders.
        LayoutSpec result = processor.process(spec(
            line("H", 10, 100, 700, 1),
            line("H", 10, 120, 700, 1)));

        assertThat(result.getItems()).hasSize(4);
        List<LayoutItem> verticals = result.getItems().stream()
            .filter(item -> "V".equals(item.getOrientation()))
            .toList();
        assertThat(verticals).extracting(LayoutItem::getLeft).containsExactlyInAnyOrder(10, 710);
        assertThat(verticals).allSatisfy(border -> {
            assertThat(border.getTop()).isEqualTo(100);
            assertThat(border.getHeight()).isEqualTo(20);
        });
    }

    @Test
    void leavesInputSpecUnchangedAndCarriesPasSpecOver() {
        LayoutItem original = text("C", 103, 400, 47, 20);
        LayoutSpec input = spec(text("A", 100, 200, 50, 20), text("B", 100, 300, 50, 20), original);
        PasSpec pasSpec = new PasSpec();
        input.setPas(pasSpec);

        LayoutSpec result = processor.process(input);

        assertThat(original.getLeft()).isEqualTo(103); // input items are deep-copied, not mutated
        assertThat(byName(result, "C").getLeft()).isEqualTo(100);
        assertThat(result.getPas()).isSameAs(pasSpec);
    }

    private LayoutSpec spec(LayoutItem... items) {
        LayoutSpec spec = new LayoutSpec();
        spec.setItems(List.of(items));
        return spec;
    }

    private LayoutItem item(String type, int left, int top, int width, int height) {
        LayoutItem item = new LayoutItem();
        item.setType(type);
        item.setLeft(left);
        item.setTop(top);
        item.setWidth(width);
        item.setHeight(height);
        return item;
    }

    private LayoutItem text(String name, int left, int top, int width, int height) {
        LayoutItem item = item("Text", left, top, width, height);
        item.setName(name);
        item.setCaption(name);
        return item;
    }

    private LayoutItem line(String orientation, int left, int top, int width, int height) {
        LayoutItem item = item("Line", left, top, width, height);
        item.setOrientation(orientation);
        item.setThickness(1);
        return item;
    }

    private LayoutItem rect(String name, int left, int top, int width, int height) {
        LayoutItem item = item("Rect", left, top, width, height);
        item.setName(name);
        return item;
    }

    private LayoutItem byName(LayoutSpec spec, String name) {
        return spec.getItems().stream()
            .filter(item -> name.equals(item.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("item not found: " + name));
    }
}
