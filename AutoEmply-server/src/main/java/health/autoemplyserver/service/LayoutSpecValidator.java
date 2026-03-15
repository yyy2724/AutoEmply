package health.autoemplyserver.service;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LayoutSpecValidator {

    private static final Pattern HEX_COLOR_REGEX = Pattern.compile("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$");
    private static final Pattern DELPHI_HEX_COLOR_REGEX = Pattern.compile("^\\$[0-9A-Fa-f]{8}$");
    private static final Pattern DELPHI_COLOR_NAME_REGEX = Pattern.compile("^cl[A-Za-z0-9_]+$");

    public List<String> validate(String formName, LayoutSpec layoutSpec) {
        List<String> errors = new ArrayList<>();

        if (formName == null || formName.isBlank()) {
            errors.add("formName is required.");
        } else if (formName.replace("_", "").isBlank()) {
            errors.add("formName must include at least one non-underscore character.");
        }

        if (layoutSpec == null) {
            errors.add("layoutSpec is required.");
            return errors;
        }

        if (layoutSpec.getItems() == null || layoutSpec.getItems().isEmpty()) {
            errors.add("layoutSpec.items must contain at least one item.");
            return errors;
        }

        Set<String> duplicates = new HashSet<>();
        int duplicateCount = 0;

        for (int index = 0; index < layoutSpec.getItems().size(); index++) {
            LayoutItem item = layoutSpec.getItems().get(index);
            String path = "layoutSpec.items[" + index + "]";
            String normalizedType = normalizeType(item.getType());

            if (normalizedType == null) {
                errors.add(path + ".type must be one of Text, Line, Rect, Image.");
                continue;
            }
            if (item.getWidth() <= 0 || item.getHeight() <= 0 || item.getLeft() < 0 || item.getTop() < 0) {
                errors.add(path + " has invalid coordinates or size.");
            }
            if ("Line".equals(normalizedType) && item.getWidth() == item.getHeight() && (item.getOrientation() == null || item.getOrientation().isBlank())) {
                errors.add(path + ".orientation must be H or V for square line items.");
            }
            if (!isValidColorOrEmpty(item.getTextColor())) {
                errors.add(path + ".textColor is invalid.");
            }
            if (!isValidColorOrEmpty(item.getStrokeColor())) {
                errors.add(path + ".strokeColor is invalid.");
            }
            if (!isValidColorOrEmpty(item.getFillColor())) {
                errors.add(path + ".fillColor is invalid.");
            }

            String duplicateKey = String.join("|",
                normalizedType,
                String.valueOf(item.getLeft()),
                String.valueOf(item.getTop()),
                String.valueOf(item.getWidth()),
                String.valueOf(item.getHeight()),
                String.valueOf(item.getCaption()),
                String.valueOf(item.getAlign()),
                String.valueOf(item.getOrientation()));
            if (!duplicates.add(duplicateKey)) {
                duplicateCount++;
            }
        }

        int duplicateLimit = Math.max(20, (int) Math.round(layoutSpec.getItems().size() * 0.4d));
        if (duplicateCount > duplicateLimit) {
            errors.add("layoutSpec contains too many duplicate items.");
        }

        return errors;
    }

    private boolean isValidColorOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String color = value.trim();
        return HEX_COLOR_REGEX.matcher(color).matches()
            || DELPHI_HEX_COLOR_REGEX.matcher(color).matches()
            || DELPHI_COLOR_NAME_REGEX.matcher(color).matches();
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        String compact = rawType.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        return switch (compact) {
            case "text", "label", "caption", "memo", "string" -> "Text";
            case "line", "hline", "vline", "horline", "vertline", "horizontalline", "verticalline" -> "Line";
            case "rect", "rectangle", "box", "cell", "tablecell" -> "Rect";
            case "image", "img", "picture", "photo", "logo" -> "Image";
            default -> null;
        };
    }
}
