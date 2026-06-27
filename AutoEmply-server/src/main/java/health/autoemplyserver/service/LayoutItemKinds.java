package health.autoemplyserver.service;

import health.autoemplyserver.model.LayoutItem;
import java.util.Objects;

/**
 * Single responsibility: classifying {@link LayoutItem}s by type/orientation. Shared,
 * stateless predicates used by {@link LayoutPostProcessor} and its collaborators
 * ({@link LayoutGeometryNormalizer}, {@link TableBorderCompleter}).
 */
final class LayoutItemKinds {

    private LayoutItemKinds() {
    }

    static boolean isText(LayoutItem item) {
        return "text".equalsIgnoreCase(item.getType());
    }

    static boolean isLine(LayoutItem item) {
        return "line".equalsIgnoreCase(item.getType());
    }

    static boolean isHorizontalLine(LayoutItem item) {
        return isLine(item) && ("H".equalsIgnoreCase(item.getOrientation()) || item.getWidth() > item.getHeight());
    }

    static boolean isVerticalLine(LayoutItem item) {
        return isLine(item) && ("V".equalsIgnoreCase(item.getOrientation()) || item.getHeight() > item.getWidth());
    }

    static String normalizeOrientation(LayoutItem item) {
        if (isHorizontalLine(item)) {
            return "H";
        }
        if (isVerticalLine(item)) {
            return "V";
        }
        return Objects.toString(item.getOrientation(), "").trim().toUpperCase();
    }
}
