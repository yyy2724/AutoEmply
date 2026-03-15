package health.autoemplyserver.service;

import health.autoemplyserver.model.FormSection;
import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.TableCell;
import health.autoemplyserver.model.TableDef;
import health.autoemplyserver.model.TableRow;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FormStructureValidator {

    public List<String> validate(FormStructure structure) {
        List<String> errors = new ArrayList<>();
        if (structure == null) {
            errors.add("FormStructure is null.");
            return errors;
        }
        if (structure.getSections() == null || structure.getSections().isEmpty()) {
            errors.add("FormStructure must have at least one section.");
            return errors;
        }

        normalizeInPlace(structure);

        for (int index = 0; index < structure.getSections().size(); index++) {
            validateSection(structure.getSections().get(index), "sections[" + index + "]", errors);
        }
        return errors;
    }

    public void normalizeInPlace(FormStructure structure) {
        for (FormSection section : structure.getSections()) {
            if (section.getTable() == null) {
                continue;
            }
            TableDef table = section.getTable();
            double totalFraction = table.getColumns().stream().mapToDouble(column -> column.getWidthFraction()).sum();
            if (totalFraction > 0 && Math.abs(totalFraction - 1.0d) > 0.001d) {
                for (var column : table.getColumns()) {
                    column.setWidthFraction(column.getWidthFraction() / totalFraction);
                }
            }
            for (TableRow row : table.getRows()) {
                for (TableCell cell : row.getCells()) {
                    if (cell.getColSpan() < 1) {
                        cell.setColSpan(1);
                    }
                    if (cell.getRowSpan() < 1) {
                        cell.setRowSpan(1);
                    }
                }
                int columnCount = table.getColumns().size();
                int totalColSpan = row.getCells().stream().mapToInt(TableCell::getColSpan).sum();
                if (totalColSpan < columnCount && !row.getCells().isEmpty()) {
                    row.getCells().getLast().setColSpan(row.getCells().getLast().getColSpan() + (columnCount - totalColSpan));
                } else if (totalColSpan > columnCount) {
                    int excess = totalColSpan - columnCount;
                    for (int idx = row.getCells().size() - 1; idx >= 0 && excess > 0; idx--) {
                        TableCell cell = row.getCells().get(idx);
                        int canShrink = cell.getColSpan() - 1;
                        int shrink = Math.min(canShrink, excess);
                        cell.setColSpan(cell.getColSpan() - shrink);
                        excess -= shrink;
                    }
                }
            }
        }
    }

    private void validateSection(FormSection section, String path, List<String> errors) {
        String sectionType = section.getSectionType() == null ? "table" : section.getSectionType().toLowerCase();
        if ("table".equals(sectionType)) {
            if (section.getTable() == null) {
                errors.add(path + ": table section must have a table definition.");
                return;
            }
            if (section.getTable().getColumns().isEmpty()) {
                errors.add(path + ".table: must have at least one column.");
            }
            if (section.getTable().getRows().isEmpty()) {
                errors.add(path + ".table: must have at least one row.");
            }
        } else if (("freeform".equals(sectionType) || "keyvalue".equals(sectionType))
            && (section.getElements() == null || section.getElements().isEmpty())) {
            errors.add(path + ": freeform/keyvalue section must have at least one element.");
        }
    }
}
