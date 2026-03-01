using AutoEmply.Models;

namespace AutoEmply.Services;

public static class FormStructureValidator
{
    /// <summary>
    /// Validates the FormStructure. Only returns hard errors that cannot be auto-corrected.
    /// Soft issues (colSpan mismatch, fraction sum) are auto-fixed by NormalizeInPlace.
    /// </summary>
    public static List<string> Validate(FormStructure? structure)
    {
        var errors = new List<string>();

        if (structure is null)
        {
            errors.Add("FormStructure is null.");
            return errors;
        }

        if (structure.Sections.Count == 0)
        {
            errors.Add("FormStructure must have at least one section.");
            return errors;
        }

        // Auto-fix soft issues before validation
        NormalizeInPlace(structure);

        for (var i = 0; i < structure.Sections.Count; i++)
        {
            ValidateSection(structure.Sections[i], $"sections[{i}]", errors);
        }

        return errors;
    }

    /// <summary>
    /// Auto-corrects common Claude mistakes in-place:
    /// - Normalize column widthFractions to sum to 1.0
    /// - Fix colSpan totals per row
    /// - Ensure colSpan/rowSpan >= 1
    /// </summary>
    public static void NormalizeInPlace(FormStructure structure)
    {
        foreach (var section in structure.Sections)
        {
            if (section.Table is null) continue;
            var table = section.Table;

            // 1. Normalize column fractions to sum to 1.0
            var totalFraction = table.Columns.Sum(c => c.WidthFraction);
            if (totalFraction > 0 && Math.Abs(totalFraction - 1.0) > 0.001)
            {
                var factor = 1.0 / totalFraction;
                foreach (var col in table.Columns)
                    col.WidthFraction *= factor;
            }

            // 2. Fix colSpan/rowSpan minimums
            foreach (var row in table.Rows)
            {
                foreach (var cell in row.Cells)
                {
                    if (cell.ColSpan < 1) cell.ColSpan = 1;
                    if (cell.RowSpan < 1) cell.RowSpan = 1;
                }
            }

            // 3. Fix colSpan totals per row
            var colCount = table.Columns.Count;
            foreach (var row in table.Rows)
            {
                var totalColSpan = row.Cells.Sum(c => c.ColSpan);

                if (totalColSpan < colCount)
                {
                    // Too few: expand the last cell's colSpan
                    var deficit = colCount - totalColSpan;
                    row.Cells[^1].ColSpan += deficit;
                }
                else if (totalColSpan > colCount)
                {
                    // Too many: shrink from the end
                    var excess = totalColSpan - colCount;
                    for (var i = row.Cells.Count - 1; i >= 0 && excess > 0; i--)
                    {
                        var canShrink = row.Cells[i].ColSpan - 1;
                        var shrink = Math.Min(canShrink, excess);
                        row.Cells[i].ColSpan -= shrink;
                        excess -= shrink;
                    }

                    // If still excess, remove trailing cells
                    while (row.Cells.Sum(c => c.ColSpan) > colCount && row.Cells.Count > 1)
                    {
                        row.Cells.RemoveAt(row.Cells.Count - 1);
                    }

                    // Final adjust on last cell
                    var remaining = colCount - row.Cells.Sum(c => c.ColSpan);
                    if (remaining > 0)
                        row.Cells[^1].ColSpan += remaining;
                    else if (remaining < 0 && row.Cells[^1].ColSpan > 1)
                        row.Cells[^1].ColSpan = Math.Max(1, row.Cells[^1].ColSpan + remaining);
                }
            }
        }
    }

    private static void ValidateSection(FormSection section, string path, List<string> errors)
    {
        var sectionType = (section.SectionType ?? "table").ToLowerInvariant();

        if (sectionType == "table")
        {
            ValidateTableSection(section, path, errors);
        }
        else if (sectionType is "freeform" or "keyvalue")
        {
            if (section.Elements is null or { Count: 0 })
            {
                errors.Add($"{path}: freeform/keyvalue section must have at least one element.");
            }
        }
    }

    private static void ValidateTableSection(FormSection section, string path, List<string> errors)
    {
        if (section.Table is null)
        {
            errors.Add($"{path}: table section must have a table definition.");
            return;
        }

        var table = section.Table;

        if (table.Columns.Count == 0)
        {
            errors.Add($"{path}.table: must have at least one column.");
            return;
        }

        if (table.Rows.Count == 0)
        {
            errors.Add($"{path}.table: must have at least one row.");
            return;
        }

        // After NormalizeInPlace, only check for truly broken rows
        for (var r = 0; r < table.Rows.Count; r++)
        {
            if (table.Rows[r].Cells.Count == 0)
            {
                errors.Add($"{path}.table.rows[{r}]: row must have at least one cell.");
            }
        }
    }
}
