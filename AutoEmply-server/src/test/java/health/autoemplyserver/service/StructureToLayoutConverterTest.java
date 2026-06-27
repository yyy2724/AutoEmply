package health.autoemplyserver.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.List;
import org.junit.jupiter.api.Test;

class StructureToLayoutConverterTest {

    private final StructureToLayoutConverter converter = new StructureToLayoutConverter();

    @Test
    void rendersTitleAsCenteredBoldLabel() {
        FormStructure structure = new FormStructure();
        structure.setTitle("직원 명부");

        LayoutSpec spec = converter.convert(structure);

        assertThat(spec.getItems()).hasSize(1);
        LayoutItem title = spec.getItems().getFirst();
        assertThat(title.getType()).isEqualTo("Text");
        assertThat(title.getName()).isEqualTo("Qlb3_TITLE");
        assertThat(title.getCaption()).isEqualTo("직원 명부");
        assertThat(title.getLeft()).isEqualTo(203);   // canvas left 10 + content width 774 / 4
        assertThat(title.getTop()).isEqualTo(6);
        assertThat(title.getWidth()).isEqualTo(387);  // half of the content width
        assertThat(title.getHeight()).isEqualTo(28);
        assertThat(title.getFontSize()).isEqualTo(20);
        assertThat(title.getBold()).isTrue();
        assertThat(title.getAlign()).isEqualTo("Center");
    }

    @Test
    void rendersMinimalTableAsGridLinesAndPaddedCellTexts() {
        FormStructure structure = structureWith(tableSection(
            List.of(column(0.5), column(0.5)),
            List.of(row(cell("R1C1"), cell("R1C2")), row(cell("R2C1"), cell("R2C2")))));

        LayoutSpec spec = converter.convert(structure);

        List<LayoutItem> horizontals = lines(spec, "H");
        assertThat(horizontals).hasSize(3); // rows + 1
        assertThat(horizontals).extracting(LayoutItem::getTop).containsExactlyInAnyOrder(6, 26, 46);
        assertThat(horizontals).extracting(LayoutItem::getWidth).containsOnly(774);

        List<LayoutItem> verticals = lines(spec, "V");
        assertThat(verticals).hasSize(3); // columns + 1
        assertThat(verticals).extracting(LayoutItem::getLeft).containsExactlyInAnyOrder(10, 397, 784);
        assertThat(verticals).extracting(LayoutItem::getHeight).containsOnly(40);

        List<LayoutItem> texts = ofType(spec, "Text");
        assertThat(texts).hasSize(4);
        LayoutItem firstCell = byCaption(spec, "R1C1");
        assertThat(firstCell.getLeft()).isEqualTo(14);  // column left 10 + cell padding 4
        assertThat(firstCell.getTop()).isEqualTo(9);    // row top 6 + cell padding 3
        assertThat(firstCell.getWidth()).isEqualTo(379); // column width 387 - 2 * padding 4
    }

    @Test
    void extractsFieldCodeFromFieldNameForComponentName() {
        TableCell named = cell("이름");
        named.setFieldName("emp_AT0001");
        TableCell lowercase = cell("코드");
        lowercase.setFieldName("code_at12");
        FormStructure structure = structureWith(tableSection(
            List.of(column(1.0)), List.of(row(named), row(lowercase))));

        LayoutSpec spec = converter.convert(structure);

        assertThat(byCaption(spec, "이름").getName()).isEqualTo("Qlb3_AT0001");
        assertThat(byCaption(spec, "코드").getName()).isEqualTo("Qlb3_AT12"); // code is upper-cased
    }

    @Test
    void componentNameFallsBackToSanitizedTextOrNull() {
        FormStructure structure = structureWith(tableSection(
            List.of(column(1.0)), List.of(row(cell("Hello World!")), row(cell("성명")))));

        LayoutSpec spec = converter.convert(structure);

        assertThat(byCaption(spec, "Hello World!").getName()).isEqualTo("Qlb3_Hello_World");
        // Korean-only text leaves nothing usable after sanitizing, so no name is assigned.
        assertThat(byCaption(spec, "성명").getName()).isNull();
    }

    @Test
    void headerRowGetsHeaderBackgroundRect() {
        TableRow header = row(cell("제목"));
        header.setHeaderRow(true);
        FormStructure structure = structureWith(tableSection(List.of(column(1.0)), List.of(header)));

        LayoutSpec spec = converter.convert(structure);

        List<LayoutItem> rects = ofType(spec, "Rect");
        assertThat(rects).hasSize(1);
        LayoutItem background = rects.getFirst();
        assertThat(background.getFillColor()).isEqualTo("#D8E4F0");
        assertThat(background.getFilled()).isTrue();
        assertThat(background.getLeft()).isEqualTo(11);   // inset by 1px inside the border
        assertThat(background.getTop()).isEqualTo(7);
        assertThat(background.getWidth()).isEqualTo(773);
        assertThat(background.getHeight()).isEqualTo(19); // standard row height 20 - 1
    }

    @Test
    void rowHeightHintsControlRowPositions() {
        FormStructure structure = structureWith(tableSection(
            List.of(column(1.0)),
            List.of(rowWithHint("compact"), rowWithHint("tall"), rowWithHint("50"),
                rowWithHint("weird"), rowWithHint("999"))));

        LayoutSpec spec = converter.convert(structure);

        // compact=14, tall=30, "50"=50, unknown hint falls back to 20, "999" clamps to 300.
        assertThat(lines(spec, "H")).extracting(LayoutItem::getTop)
            .containsExactlyInAnyOrder(6, 20, 50, 100, 120, 420);
    }

    @Test
    void convertsFreeformLineAndImageElements() {
        FreeformElement line = freeform("line", 0.0, 0.0, 0.5, 0.02);
        FreeformElement image = freeform("image", 0.5, 0.0, 0.25, 0.1);
        FormSection section = new FormSection();
        section.setSectionType("freeform");
        section.setElements(List.of(line, image));

        LayoutSpec spec = converter.convert(structureWith(section));

        List<LayoutItem> lines = lines(spec, "H");
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getLeft()).isEqualTo(10);
        assertThat(lines.getFirst().getTop()).isEqualTo(6);
        assertThat(lines.getFirst().getWidth()).isEqualTo(387);

        List<LayoutItem> images = ofType(spec, "Image");
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getLeft()).isEqualTo(397);
        assertThat(images.getFirst().getWidth()).isEqualTo(194);
        assertThat(images.getFirst().getHeight()).isEqualTo(13); // raised to the minimum label height
        assertThat(images.getFirst().getStretch()).isTrue();
    }

    @Test
    void rendersFooterTextAndSignatureLine() {
        FooterElement text = footer("text", "담당자", 0.0, 0.25);
        FooterElement signature = footer("signature_line", null, 0.5, 0.3);
        FormStructure structure = new FormStructure();
        structure.setFooter(List.of(text, signature));

        LayoutSpec spec = converter.convert(structure);

        LayoutItem label = byCaption(spec, "담당자");
        assertThat(label.getLeft()).isEqualTo(10);
        assertThat(label.getTop()).isEqualTo(10);   // start 6 + footer gap 4
        assertThat(label.getWidth()).isEqualTo(194);

        List<LayoutItem> lines = lines(spec, "H");
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().getLeft()).isEqualTo(397);
        assertThat(lines.getFirst().getTop()).isEqualTo(35); // label bottom 25 + 10px offset
        assertThat(lines.getFirst().getWidth()).isEqualTo(232);
    }

    private FormStructure structureWith(FormSection... sections) {
        FormStructure structure = new FormStructure();
        structure.setSections(List.of(sections));
        return structure;
    }

    private FormSection tableSection(List<ColumnDef> columns, List<TableRow> rows) {
        TableDef table = new TableDef();
        table.setColumns(columns);
        table.setRows(rows);
        FormSection section = new FormSection();
        section.setSectionType("table");
        section.setTable(table);
        return section;
    }

    private ColumnDef column(double widthFraction) {
        ColumnDef column = new ColumnDef();
        column.setWidthFraction(widthFraction);
        return column;
    }

    private TableRow row(TableCell... cells) {
        TableRow row = new TableRow();
        row.setCells(List.of(cells));
        return row;
    }

    private TableRow rowWithHint(String heightHint) {
        TableRow row = new TableRow();
        row.setHeightHint(heightHint);
        return row;
    }

    private TableCell cell(String text) {
        TableCell cell = new TableCell();
        cell.setText(text);
        return cell;
    }

    private FreeformElement freeform(String elementType, double x, double y, double width, double height) {
        FreeformElement element = new FreeformElement();
        element.setElementType(elementType);
        element.setXFraction(x);
        element.setYFraction(y);
        element.setWidthFraction(width);
        element.setHeightFraction(height);
        return element;
    }

    private FooterElement footer(String elementType, String text, double x, double width) {
        FooterElement element = new FooterElement();
        element.setElementType(elementType);
        element.setText(text);
        element.setXFraction(x);
        element.setWidthFraction(width);
        return element;
    }

    private List<LayoutItem> ofType(LayoutSpec spec, String type) {
        return spec.getItems().stream().filter(item -> type.equals(item.getType())).toList();
    }

    private List<LayoutItem> lines(LayoutSpec spec, String orientation) {
        return ofType(spec, "Line").stream()
            .filter(item -> orientation.equals(item.getOrientation()))
            .toList();
    }

    private LayoutItem byCaption(LayoutSpec spec, String caption) {
        return spec.getItems().stream()
            .filter(item -> caption.equals(item.getCaption()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("item not found: " + caption));
    }
}
