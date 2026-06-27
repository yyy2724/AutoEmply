package health.autoemplyserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import health.autoemplyserver.model.ColumnDef;
import health.autoemplyserver.model.FormSection;
import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.TableCell;
import health.autoemplyserver.model.TableDef;
import health.autoemplyserver.model.TableRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormStructureValidatorTest {

    private final FormStructureValidator validator = new FormStructureValidator();

    @Test
    void nullStructureIsRejected() {
        assertThat(validator.validate(null)).containsExactly("FormStructure is null.");
    }

    @Test
    void structureWithoutSectionsIsRejected() {
        assertThat(validator.validate(new FormStructure()))
            .containsExactly("FormStructure must have at least one section.");
    }

    @Test
    void minimalTableSectionPassesValidation() {
        FormStructure structure = structureWith(
            tableSection(table(List.of(column(1.0)), List.of(row(cell("이름", 1))))));

        assertThat(validator.validate(structure)).isEmpty();
    }

    @Test
    void tableSectionWithoutTableDefinitionIsRejected() {
        FormSection section = new FormSection(); // sectionType defaults to "table", table stays null

        assertThat(validator.validate(structureWith(section)))
            .containsExactly("sections[0]: table section must have a table definition.");
    }

    @Test
    void tableWithoutColumnsAndRowsReportsBothErrors() {
        FormStructure structure = structureWith(tableSection(new TableDef()));

        assertThat(validator.validate(structure)).containsExactly(
            "sections[0].table: must have at least one column.",
            "sections[0].table: must have at least one row.");
    }

    @Test
    void freeformSectionWithoutElementsIsRejected() {
        FormSection section = new FormSection();
        section.setSectionType("freeform");

        assertThat(validator.validate(structureWith(section)))
            .containsExactly("sections[0]: freeform/keyvalue section must have at least one element.");
    }

    @Test
    void columnWidthFractionsAreNormalizedToSumOne() {
        ColumnDef narrow = column(1.0);
        ColumnDef wide = column(3.0);
        FormStructure structure = structureWith(
            tableSection(table(List.of(narrow, wide), List.of(row(cell("a", 2))))));

        assertThat(validator.validate(structure)).isEmpty();
        assertThat(narrow.getWidthFraction()).isCloseTo(0.25, within(1e-9));
        assertThat(wide.getWidthFraction()).isCloseTo(0.75, within(1e-9));
    }

    @Test
    void nonPositiveCellSpansAreRaisedToOne() {
        TableCell broken = cell("a", 0);
        broken.setRowSpan(-1);
        FormStructure structure = structureWith(
            tableSection(table(List.of(column(1.0)), List.of(row(broken)))));

        assertThat(validator.validate(structure)).isEmpty();
        assertThat(broken.getColSpan()).isEqualTo(1);
        assertThat(broken.getRowSpan()).isEqualTo(1);
    }

    @Test
    void rowWithTooFewColSpansIsPaddedOnLastCell() {
        TableCell first = cell("a", 1);
        TableCell last = cell("b", 1);
        FormStructure structure = structureWith(
            tableSection(table(List.of(column(0.2), column(0.3), column(0.5)), List.of(row(first, last)))));

        validator.validate(structure);

        assertThat(first.getColSpan()).isEqualTo(1);
        assertThat(last.getColSpan()).isEqualTo(2); // padded so spans cover all 3 columns
    }

    @Test
    void rowWithExcessColSpansIsShrunkFromLastCellBackwards() {
        TableCell first = cell("a", 2);
        TableCell last = cell("b", 3);
        FormStructure structure = structureWith(
            tableSection(table(List.of(column(0.5), column(0.5)), List.of(row(first, last)))));

        validator.validate(structure);

        assertThat(first.getColSpan()).isEqualTo(1);
        assertThat(last.getColSpan()).isEqualTo(1);
    }

    private FormStructure structureWith(FormSection... sections) {
        FormStructure structure = new FormStructure();
        structure.setSections(List.of(sections));
        return structure;
    }

    private FormSection tableSection(TableDef table) {
        FormSection section = new FormSection();
        section.setSectionType("table");
        section.setTable(table);
        return section;
    }

    private TableDef table(List<ColumnDef> columns, List<TableRow> rows) {
        TableDef table = new TableDef();
        table.setColumns(columns);
        table.setRows(rows);
        return table;
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

    private TableCell cell(String text, int colSpan) {
        TableCell cell = new TableCell();
        cell.setText(text);
        cell.setColSpan(colSpan);
        return cell;
    }
}
