package health.autoemplyserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutSpecValidatorTest {

    private final LayoutSpecValidator validator = new LayoutSpecValidator();

    @Test
    void acceptsKnownTypeAliasesAndAllSupportedColorFormats() {
        LayoutItem label = item("label", 10, 10, 100, 13);
        label.setTextColor("#112233");          // #RRGGBB
        LayoutItem box = item("box", 10, 40, 100, 30);
        box.setFillColor("#112233CC");          // #RRGGBBAA
        LayoutItem verticalLine = item("vertical-line", 200, 10, 1, 50);
        verticalLine.setStrokeColor("$00D8E4F0"); // Delphi $00BBGGRR hex
        LayoutItem photo = item("photo", 300, 10, 80, 60);
        photo.setTextColor("clBtnFace");        // Delphi named color

        List<String> errors = validator.validate("Form_QREmply25", specOf(label, box, verticalLine, photo));

        assertThat(errors).isEmpty();
    }

    @Test
    void rejectsMissingOrBlankFormName() {
        assertThat(validator.validate(null, validSpec())).containsExactly("formName is required.");
        assertThat(validator.validate("   ", validSpec())).containsExactly("formName is required.");
    }

    @Test
    void rejectsUnderscoreOnlyFormName() {
        assertThat(validator.validate("___", validSpec()))
            .containsExactly("formName must include at least one non-underscore character.");
    }

    @Test
    void rejectsNullLayoutSpec() {
        assertThat(validator.validate("Form_X", null)).containsExactly("layoutSpec is required.");
    }

    @Test
    void rejectsEmptyItemList() {
        assertThat(validator.validate("Form_X", specOf()))
            .containsExactly("layoutSpec.items must contain at least one item.");
    }

    @Test
    void rejectsUnknownItemType() {
        List<String> errors = validator.validate("Form_X", specOf(item("circle", 10, 10, 50, 50)));

        assertThat(errors).containsExactly("layoutSpec.items[0].type must be one of Text, Line, Rect, Image.");
    }

    @Test
    void rejectsNonPositiveSizeAndNegativePosition() {
        List<String> errors = validator.validate("Form_X", specOf(
            item("Text", 10, 10, 0, 13),
            item("Text", -1, 10, 50, 13)));

        assertThat(errors).containsExactly(
            "layoutSpec.items[0] has invalid coordinates or size.",
            "layoutSpec.items[1] has invalid coordinates or size.");
    }

    @Test
    void requiresOrientationForSquareLineItems() {
        LayoutItem squareLine = item("Line", 10, 10, 10, 10);

        assertThat(validator.validate("Form_X", specOf(squareLine)))
            .containsExactly("layoutSpec.items[0].orientation must be H or V for square line items.");

        squareLine.setOrientation("H");
        assertThat(validator.validate("Form_X", specOf(squareLine))).isEmpty();
    }

    @Test
    void rejectsMalformedColorValues() {
        LayoutItem item = item("Text", 10, 10, 100, 13);
        item.setTextColor("#FFF");      // too few hex digits
        item.setStrokeColor("red");     // css name is not supported
        item.setFillColor("$FF0000");   // Delphi hex needs exactly 8 digits

        List<String> errors = validator.validate("Form_X", specOf(item));

        assertThat(errors).containsExactly(
            "layoutSpec.items[0].textColor is invalid.",
            "layoutSpec.items[0].strokeColor is invalid.",
            "layoutSpec.items[0].fillColor is invalid.");
    }

    @Test
    void flagsSpecWithExcessiveDuplicateItems() {
        // 25 identical items -> 24 duplicates, above the limit max(20, 40% of 25) = 20.
        List<String> errors = validator.validate("Form_X", specOf(identicalItems(25)));

        assertThat(errors).containsExactly("layoutSpec contains too many duplicate items.");
    }

    @Test
    void allowsDuplicatesUpToTheLimit() {
        // 21 identical items -> exactly 20 duplicates, which is still within the limit.
        List<String> errors = validator.validate("Form_X", specOf(identicalItems(21)));

        assertThat(errors).isEmpty();
    }

    private LayoutItem[] identicalItems(int count) {
        List<LayoutItem> items = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            items.add(item("Text", 10, 10, 100, 13));
        }
        return items.toArray(LayoutItem[]::new);
    }

    private LayoutSpec validSpec() {
        return specOf(item("Text", 10, 10, 100, 13));
    }

    private LayoutSpec specOf(LayoutItem... items) {
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
}
