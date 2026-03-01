using AutoEmply.Models;
using System.Text.RegularExpressions;

namespace AutoEmply.Services;

public sealed class StructureToLayoutConverter
{
    private const int CanvasWidth = 794;
    private const int CanvasLeft = 10;
    private const int CanvasRight = 784;
    private const int ContentWidth = 774; // CanvasRight - CanvasLeft

    private const int StandardRowHeight = 20;
    private const int CompactRowHeight = 14;
    private const int TallRowHeight = 30;
    private const int LabelHeight = 13;
    private const int TitleHeight = 28;
    private const int CellPaddingLeft = 4;
    private const int CellPaddingTop = 3;
    private const int SectionGap = 2;

    private const string HeaderBgColor = "#D8E4F0";
    private const string BorderColor = "#000000";
    private static readonly Regex InternalComponentNameRegex = new("^(Q(?:lb|sp|sh|img)\\d*_[A-Za-z0-9_]+)$", RegexOptions.Compiled | RegexOptions.IgnoreCase);
    private static readonly Regex FieldCodeRegex = new("([A-Za-z]{2,4}\\d{2,4})", RegexOptions.Compiled);

    public LayoutSpec Convert(FormStructure structure)
    {
        var items = new List<LayoutItem>();
        var currentY = 6;

        // Title
        if (!string.IsNullOrWhiteSpace(structure.Title))
        {
            items.Add(MakeLabel(
                structure.Title,
                left: CanvasLeft + ContentWidth / 4,
                top: currentY,
                width: ContentWidth / 2,
                height: TitleHeight,
                fontSize: structure.TitleFontSize,
                bold: true,
                align: "Center",
                name: "Qlb3_TITLE"));
            currentY += TitleHeight + 6;
        }

        // Sections
        foreach (var section in structure.Sections)
        {
            currentY = RenderSection(section, items, currentY);
            currentY += SectionGap;
        }

        // Footer
        if (structure.Footer is { Count: > 0 })
        {
            currentY += 4;
            currentY = RenderFooter(structure.Footer, items, currentY);
        }

        return new LayoutSpec { Items = items };
    }

    private int RenderSection(FormSection section, List<LayoutItem> items, int startY)
    {
        var sectionType = (section.SectionType ?? "table").ToLowerInvariant();
        return sectionType switch
        {
            "table" => RenderTableSection(section, items, startY),
            "freeform" or "keyvalue" => RenderFreeformSection(section, items, startY),
            _ => RenderFreeformSection(section, items, startY)
        };
    }

    private int RenderTableSection(FormSection section, List<LayoutItem> items, int startY)
    {
        var table = section.Table;
        if (table is null || table.Columns.Count == 0 || table.Rows.Count == 0)
            return startY;

        // Calculate table boundaries
        int tableLeft, tableWidth;
        if (table.FullWidth)
        {
            tableLeft = CanvasLeft;
            tableWidth = ContentWidth;
        }
        else
        {
            tableLeft = CanvasLeft + (int)Math.Round(table.LeftFraction * ContentWidth);
            tableWidth = (int)Math.Round(table.WidthFraction * ContentWidth);
        }

        var tableRight = tableLeft + tableWidth;

        // Calculate column boundaries from fractions
        var colBoundaries = CalculateColumnBoundaries(table.Columns, tableLeft, tableWidth);

        // Calculate row Y positions
        var rowYPositions = new List<int> { startY };
        foreach (var row in table.Rows)
        {
            rowYPositions.Add(rowYPositions[^1] + ResolveRowHeight(row));
        }

        var totalHeight = rowYPositions[^1] - startY;

        // --- 1. Background rectangles (rendered first for Z-order) ---

        // Header column backgrounds (full height of table)
        for (var c = 0; c < table.Columns.Count; c++)
        {
            if (table.Columns[c].IsHeaderColumn)
            {
                var colLeft = colBoundaries[c] + 1;
                var colWidth = colBoundaries[c + 1] - colBoundaries[c] - 1;
                items.Add(MakeRect(colLeft, startY + 1, colWidth, totalHeight - 1, HeaderBgColor));
            }
        }

        // Header row backgrounds
        for (var r = 0; r < table.Rows.Count; r++)
        {
            if (table.Rows[r].IsHeaderRow)
            {
                var rowHeight = rowYPositions[r + 1] - rowYPositions[r];
                items.Add(MakeRect(tableLeft + 1, rowYPositions[r] + 1, tableWidth - 1, rowHeight - 1, HeaderBgColor));
            }
            else
            {
                // Individual cell backgrounds
                var cellColIndex = 0;
                foreach (var cell in table.Rows[r].Cells)
                {
                    if (cell.HasBackground && cellColIndex < colBoundaries.Length - 1)
                    {
                        var spanEnd = Math.Min(cellColIndex + Math.Max(1, cell.ColSpan), colBoundaries.Length - 1);
                        var cellLeft = colBoundaries[cellColIndex] + 1;
                        var cellWidth = colBoundaries[spanEnd] - colBoundaries[cellColIndex] - 1;
                        var rowHeight = rowYPositions[r + 1] - rowYPositions[r];
                        items.Add(MakeRect(cellLeft, rowYPositions[r] + 1, cellWidth, rowHeight - 1, HeaderBgColor));
                    }

                    cellColIndex += Math.Max(1, cell.ColSpan);
                }
            }
        }

        // --- 2. Border lines ---

        // Horizontal lines: top, bottom, and between rows
        for (var r = 0; r <= table.Rows.Count; r++)
        {
            items.Add(MakeHLine(tableLeft, rowYPositions[r], tableWidth));
        }

        // Vertical lines: left, right, and between columns
        for (var c = 0; c <= table.Columns.Count; c++)
        {
            var x = c < colBoundaries.Length ? colBoundaries[c] : tableRight;
            items.Add(MakeVLine(x, startY, totalHeight));
        }

        // --- 3. Cell text labels ---
        for (var r = 0; r < table.Rows.Count; r++)
        {
            var row = table.Rows[r];
            var cellColIndex = 0;

            foreach (var cell in row.Cells)
            {
                if (cellColIndex >= colBoundaries.Length - 1) break;

                var colSpan = Math.Max(1, cell.ColSpan);
                var spanEnd = Math.Min(cellColIndex + colSpan, colBoundaries.Length - 1);

                var cellLeft = colBoundaries[cellColIndex] + CellPaddingLeft;
                var cellTop = rowYPositions[r] + CellPaddingTop;
                var cellWidth = colBoundaries[spanEnd] - colBoundaries[cellColIndex] - (CellPaddingLeft * 2);
                var cellHeight = LabelHeight;

                // Prefer visible OCR text. fieldName is often an internal token (e.g., Qlb3_PTNCO).
                var displayText = ResolveDisplayText(cell.Text, cell.FieldName);
                var componentName = ResolveComponentName(displayText, cell.FieldName);

                if (!string.IsNullOrWhiteSpace(displayText))
                {
                    items.Add(MakeLabel(
                        displayText,
                        cellLeft, cellTop,
                        Math.Max(1, cellWidth), cellHeight,
                        cell.FontSize > 0 ? cell.FontSize : 9,
                        cell.Bold,
                        cell.Align,
                        cell.TextColor,
                        componentName));
                }

                cellColIndex += colSpan;
            }
        }

        return rowYPositions[^1];
    }

    private static string ResolveDisplayText(string? text, string? fieldName)
    {
        var visibleText = (text ?? string.Empty).Trim();
        if (!string.IsNullOrWhiteSpace(visibleText))
        {
            return visibleText;
        }

        var field = (fieldName ?? string.Empty).Trim();
        if (string.IsNullOrWhiteSpace(field))
        {
            return string.Empty;
        }

        // Block internal component identifiers from leaking into visible captions.
        if (InternalComponentNameRegex.IsMatch(field))
        {
            return string.Empty;
        }

        return field;
    }

    private static string? ResolveComponentName(string? displayText, string? fieldName)
    {
        var field = (fieldName ?? string.Empty).Trim();
        if (!string.IsNullOrWhiteSpace(field))
        {
            var match = FieldCodeRegex.Match(field);
            if (match.Success)
            {
                return $"Qlb3_{match.Groups[1].Value.ToUpperInvariant()}";
            }
        }

        var visibleText = (displayText ?? string.Empty).Trim();
        if (!string.IsNullOrWhiteSpace(visibleText))
        {
            var sanitizedFromText = SanitizeIdentifier(visibleText);
            if (!string.IsNullOrWhiteSpace(sanitizedFromText))
            {
                return $"Qlb3_{sanitizedFromText}";
            }
        }

        return null;
    }

    private static string SanitizeIdentifier(string raw)
    {
        static bool IsAsciiLetterOrDigit(char ch) =>
            (ch is >= 'A' and <= 'Z') || (ch is >= 'a' and <= 'z') || (ch is >= '0' and <= '9');

        var chars = raw
            .Select(ch => IsAsciiLetterOrDigit(ch) ? ch : '_')
            .ToArray();
        var candidate = new string(chars).Trim('_');
        if (string.IsNullOrWhiteSpace(candidate))
        {
            return string.Empty;
        }

        while (candidate.Contains("__", StringComparison.Ordinal))
        {
            candidate = candidate.Replace("__", "_", StringComparison.Ordinal);
        }

        if (!char.IsLetter(candidate[0]) && candidate[0] != '_')
        {
            candidate = $"N_{candidate}";
        }

        return candidate.Length > 40 ? candidate[..40] : candidate;
    }

    private int RenderFreeformSection(FormSection section, List<LayoutItem> items, int startY)
    {
        if (section.Elements is null or { Count: 0 })
            return startY;

        // Estimate section height from elements
        var maxBottomFraction = section.Elements.Max(e => e.YFraction + e.HeightFraction);
        var sectionHeight = Math.Max(StandardRowHeight, (int)Math.Round(maxBottomFraction * 200));

        foreach (var element in section.Elements)
        {
            var left = CanvasLeft + (int)Math.Round(element.XFraction * ContentWidth);
            var top = startY + (int)Math.Round(element.YFraction * sectionHeight);
            var width = Math.Max(1, (int)Math.Round(element.WidthFraction * ContentWidth));
            var height = Math.Max(LabelHeight, (int)Math.Round(element.HeightFraction * sectionHeight));

            var elementType = (element.ElementType ?? "text").ToLowerInvariant();

            switch (elementType)
            {
                case "line":
                    items.Add(MakeHLine(left, top, width));
                    break;
                case "image":
                    items.Add(new LayoutItem
                    {
                        Type = "Image",
                        Left = left, Top = top, Width = width, Height = height,
                        Stretch = true
                    });
                    break;
                default: // "text", "checkbox"
                    items.Add(MakeLabel(
                        element.Text ?? string.Empty,
                        left, top, width, LabelHeight,
                        element.FontSize > 0 ? element.FontSize : 9,
                        element.Bold,
                        element.Align));
                    break;
            }
        }

        return startY + sectionHeight;
    }

    private int RenderFooter(List<FooterElement> footerElements, List<LayoutItem> items, int startY)
    {
        var currentY = startY;
        foreach (var element in footerElements)
        {
            var left = CanvasLeft + (int)Math.Round(element.XFraction * ContentWidth);
            var width = Math.Max(1, (int)Math.Round(element.WidthFraction * ContentWidth));

            var elementType = (element.ElementType ?? "text").ToLowerInvariant();

            switch (elementType)
            {
                case "image":
                    items.Add(new LayoutItem
                    {
                        Type = "Image",
                        Left = left, Top = currentY, Width = width, Height = 30,
                        Stretch = true
                    });
                    currentY += 34;
                    break;
                case "signature_line":
                    items.Add(MakeHLine(left, currentY + 10, width));
                    currentY += 14;
                    break;
                default:
                    items.Add(MakeLabel(
                        element.Text ?? string.Empty,
                        left, currentY, width, LabelHeight,
                        element.FontSize > 0 ? element.FontSize : 9,
                        element.Bold,
                        element.Align));
                    currentY += LabelHeight + 2;
                    break;
            }
        }

        return currentY;
    }

    private static int[] CalculateColumnBoundaries(List<ColumnDef> columns, int tableLeft, int tableWidth)
    {
        // Normalize fractions to sum to exactly 1.0
        var totalFraction = columns.Sum(c => c.WidthFraction);
        var normFactor = totalFraction > 0 ? 1.0 / totalFraction : 1.0;

        var boundaries = new int[columns.Count + 1];
        boundaries[0] = tableLeft;

        var accum = 0.0;
        for (var i = 0; i < columns.Count; i++)
        {
            accum += columns[i].WidthFraction * normFactor;
            boundaries[i + 1] = tableLeft + (int)Math.Round(accum * tableWidth);
        }

        // Ensure last boundary is exact
        boundaries[^1] = tableLeft + tableWidth;
        return boundaries;
    }

    private static int ResolveRowHeight(TableRow row)
    {
        var hint = (row.HeightHint ?? "standard").Trim().ToLowerInvariant();
        return hint switch
        {
            "standard" => StandardRowHeight,
            "compact" => CompactRowHeight,
            "tall" => TallRowHeight,
            _ when int.TryParse(hint, out var h) => Math.Clamp(h, 10, 300),
            _ => StandardRowHeight
        };
    }

    private static LayoutItem MakeLabel(string caption, int left, int top, int width, int height,
        int fontSize = 9, bool bold = false, string align = "Left", string? textColor = null, string? name = null)
    {
        return new LayoutItem
        {
            Name = name,
            Type = "Text",
            Left = Math.Max(0, left),
            Top = Math.Max(0, top),
            Width = Math.Max(1, width),
            Height = Math.Max(1, height),
            Caption = caption,
            FontSize = Math.Clamp(fontSize, 6, 24),
            Bold = bold,
            Align = align,
            Transparent = true,
            TextColor = textColor ?? "#000000"
        };
    }

    private static LayoutItem MakeHLine(int left, int top, int width)
    {
        return new LayoutItem
        {
            Type = "Line",
            Left = Math.Max(0, left),
            Top = Math.Max(0, top),
            Width = Math.Max(1, width),
            Height = 1,
            Orientation = "H",
            Thickness = 1,
            StrokeColor = BorderColor
        };
    }

    private static LayoutItem MakeVLine(int left, int top, int height)
    {
        return new LayoutItem
        {
            Type = "Line",
            Left = Math.Max(0, left),
            Top = Math.Max(0, top),
            Width = 1,
            Height = Math.Max(1, height),
            Orientation = "V",
            Thickness = 1,
            StrokeColor = BorderColor
        };
    }

    private static LayoutItem MakeRect(int left, int top, int width, int height, string fillColor)
    {
        return new LayoutItem
        {
            Type = "Rect",
            Left = Math.Max(0, left),
            Top = Math.Max(0, top),
            Width = Math.Max(1, width),
            Height = Math.Max(1, height),
            FillColor = fillColor,
            Filled = true,
            StrokeColor = fillColor,
            Thickness = 1
        };
    }
}
