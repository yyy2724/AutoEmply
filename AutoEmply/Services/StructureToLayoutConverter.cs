using AutoEmply.Models;
using System.Text.RegularExpressions;

namespace AutoEmply.Services;

/// <summary>
/// FormStructure(논리 구조) → LayoutSpec(픽셀 좌표) 변환기.
///
/// Phase 1에서 Claude가 비율 기반으로 추출한 논리 구조를,
/// Phase 2에서 Delphi DFM에 쓸 수 있는 픽셀 좌표 LayoutSpec으로 변환한다.
///
/// 변환 원리:
///   - 캔버스 너비 774px (좌우 여백 10px씩 제외)
///   - 열 너비 = 비율(fraction) × 캔버스 너비
///   - 행 높이 = HeightHint에 따른 고정 값 (standard=20, compact=14, tall=30)
///   - Z-Order: Rect(배경) → Line(테두리) → Text(라벨) 순으로 배치
/// </summary>
public sealed class StructureToLayoutConverter
{
    // ── 캔버스 상수 ──
    private const int CanvasLeft = 10;
    private const int ContentWidth = 774;

    // ── 크기 상수 ──
    private const int StandardRowHeight = 20;
    private const int CompactRowHeight = 14;
    private const int TallRowHeight = 30;
    private const int LabelHeight = 13;
    private const int TitleHeight = 28;
    private const int CellPaddingLeft = 4;
    private const int CellPaddingTop = 3;
    private const int SectionGap = 2;

    // ── 스타일 상수 ──
    private const string HeaderBgColor = "#D8E4F0";
    private const string BorderColor = "#000000";

    private static readonly Regex InternalNameRegex = new("^(Q(?:lb|sp|sh|img)\\d*_[A-Za-z0-9_]+)$", RegexOptions.Compiled | RegexOptions.IgnoreCase);
    private static readonly Regex FieldCodeRegex = new("([A-Za-z]{2,4}\\d{2,4})", RegexOptions.Compiled);

    // ═══════════════════════════════════════════
    //  공개 API
    // ═══════════════════════════════════════════

    public LayoutSpec Convert(FormStructure structure)
    {
        var items = new List<LayoutItem>();
        var currentY = 6;

        // 제목
        if (!string.IsNullOrWhiteSpace(structure.Title))
        {
            items.Add(MakeLabel(structure.Title,
                CanvasLeft + ContentWidth / 4, currentY, ContentWidth / 2, TitleHeight,
                structure.TitleFontSize, bold: true, align: "Center", name: "Qlb3_TITLE"));
            currentY += TitleHeight + 6;
        }

        // 섹션들
        foreach (var section in structure.Sections)
        {
            currentY = RenderSection(section, items, currentY);
            currentY += SectionGap;
        }

        // 푸터
        if (structure.Footer is { Count: > 0 })
        {
            currentY += 4;
            RenderFooter(structure.Footer, items, currentY);
        }

        return new LayoutSpec { Items = items };
    }

    // ═══════════════════════════════════════════
    //  섹션 렌더링
    // ═══════════════════════════════════════════

    private int RenderSection(FormSection section, List<LayoutItem> items, int startY) =>
        (section.SectionType ?? "table").ToLowerInvariant() switch
        {
            "table" => RenderTableSection(section, items, startY),
            _ => RenderFreeformSection(section, items, startY)
        };

    private int RenderTableSection(FormSection section, List<LayoutItem> items, int startY)
    {
        var table = section.Table;
        if (table is null || table.Columns.Count == 0 || table.Rows.Count == 0)
            return startY;

        // 테이블 경계 계산
        var (tableLeft, tableWidth) = table.FullWidth
            ? (CanvasLeft, ContentWidth)
            : (CanvasLeft + (int)Math.Round(table.LeftFraction * ContentWidth),
               (int)Math.Round(table.WidthFraction * ContentWidth));

        var colBounds = CalculateColumnBoundaries(table.Columns, tableLeft, tableWidth);
        var rowYs = CalculateRowYPositions(table.Rows, startY);
        var totalHeight = rowYs[^1] - startY;

        // 1. 배경 사각형 (Z-Order 최하위)
        RenderBackgrounds(table, items, colBounds, rowYs, tableLeft, tableWidth, startY, totalHeight);

        // 2. 테두리 선
        RenderBorders(table, items, colBounds, rowYs, tableLeft, tableWidth, startY, totalHeight);

        // 3. 셀 텍스트
        RenderCellTexts(table, items, colBounds, rowYs);

        return rowYs[^1];
    }

    // ── 테이블: 배경 ──

    private static void RenderBackgrounds(
        TableDef table, List<LayoutItem> items, int[] colBounds, List<int> rowYs,
        int tableLeft, int tableWidth, int startY, int totalHeight)
    {
        // 헤더 열 배경 (전체 높이)
        for (var c = 0; c < table.Columns.Count; c++)
        {
            if (table.Columns[c].IsHeaderColumn)
                items.Add(MakeRect(colBounds[c] + 1, startY + 1,
                    colBounds[c + 1] - colBounds[c] - 1, totalHeight - 1, HeaderBgColor));
        }

        // 헤더 행 / 개별 셀 배경
        for (var r = 0; r < table.Rows.Count; r++)
        {
            var rowHeight = rowYs[r + 1] - rowYs[r];
            if (table.Rows[r].IsHeaderRow)
            {
                items.Add(MakeRect(tableLeft + 1, rowYs[r] + 1, tableWidth - 1, rowHeight - 1, HeaderBgColor));
                continue;
            }

            var colIndex = 0;
            foreach (var cell in table.Rows[r].Cells)
            {
                if (cell.HasBackground && colIndex < colBounds.Length - 1)
                {
                    var spanEnd = Math.Min(colIndex + Math.Max(1, cell.ColSpan), colBounds.Length - 1);
                    items.Add(MakeRect(colBounds[colIndex] + 1, rowYs[r] + 1,
                        colBounds[spanEnd] - colBounds[colIndex] - 1, rowHeight - 1, HeaderBgColor));
                }
                colIndex += Math.Max(1, cell.ColSpan);
            }
        }
    }

    // ── 테이블: 테두리 ──

    private static void RenderBorders(
        TableDef table, List<LayoutItem> items, int[] colBounds, List<int> rowYs,
        int tableLeft, int tableWidth, int startY, int totalHeight)
    {
        // 가로선: 상단, 하단, 행 사이
        for (var r = 0; r <= table.Rows.Count; r++)
            items.Add(MakeHLine(tableLeft, rowYs[r], tableWidth));

        // 세로선: 좌측, 우측, 열 사이
        var tableRight = tableLeft + tableWidth;
        for (var c = 0; c <= table.Columns.Count; c++)
        {
            var x = c < colBounds.Length ? colBounds[c] : tableRight;
            items.Add(MakeVLine(x, startY, totalHeight));
        }
    }

    // ── 테이블: 셀 텍스트 ──

    private static void RenderCellTexts(
        TableDef table, List<LayoutItem> items, int[] colBounds, List<int> rowYs)
    {
        for (var r = 0; r < table.Rows.Count; r++)
        {
            var colIndex = 0;
            foreach (var cell in table.Rows[r].Cells)
            {
                if (colIndex >= colBounds.Length - 1) break;

                var colSpan = Math.Max(1, cell.ColSpan);
                var spanEnd = Math.Min(colIndex + colSpan, colBounds.Length - 1);
                var cellLeft = colBounds[colIndex] + CellPaddingLeft;
                var cellTop = rowYs[r] + CellPaddingTop;
                var cellWidth = colBounds[spanEnd] - colBounds[colIndex] - CellPaddingLeft * 2;

                var displayText = ResolveDisplayText(cell.Text, cell.FieldName);
                var componentName = ResolveComponentName(displayText, cell.FieldName);

                if (!string.IsNullOrWhiteSpace(displayText))
                {
                    items.Add(MakeLabel(displayText, cellLeft, cellTop,
                        Math.Max(1, cellWidth), LabelHeight,
                        cell.FontSize > 0 ? cell.FontSize : 9,
                        cell.Bold, cell.Align, cell.TextColor, componentName));
                }

                colIndex += colSpan;
            }
        }
    }

    // ═══════════════════════════════════════════
    //  자유 배치 / 푸터 렌더링
    // ═══════════════════════════════════════════

    private int RenderFreeformSection(FormSection section, List<LayoutItem> items, int startY)
    {
        if (section.Elements is null or { Count: 0 }) return startY;

        var maxBottom = section.Elements.Max(e => e.YFraction + e.HeightFraction);
        var sectionHeight = Math.Max(StandardRowHeight, (int)Math.Round(maxBottom * 200));

        foreach (var el in section.Elements)
        {
            var left = CanvasLeft + (int)Math.Round(el.XFraction * ContentWidth);
            var top = startY + (int)Math.Round(el.YFraction * sectionHeight);
            var width = Math.Max(1, (int)Math.Round(el.WidthFraction * ContentWidth));
            var height = Math.Max(LabelHeight, (int)Math.Round(el.HeightFraction * sectionHeight));

            switch ((el.ElementType ?? "text").ToLowerInvariant())
            {
                case "line":
                    items.Add(MakeHLine(left, top, width));
                    break;
                case "image":
                    items.Add(new LayoutItem { Type = "Image", Left = left, Top = top, Width = width, Height = height, Stretch = true });
                    break;
                default:
                    items.Add(MakeLabel(el.Text ?? "", left, top, width, LabelHeight,
                        el.FontSize > 0 ? el.FontSize : 9, el.Bold, el.Align));
                    break;
            }
        }

        return startY + sectionHeight;
    }

    private static int RenderFooter(List<FooterElement> footerElements, List<LayoutItem> items, int startY)
    {
        var y = startY;
        foreach (var el in footerElements)
        {
            var left = CanvasLeft + (int)Math.Round(el.XFraction * ContentWidth);
            var width = Math.Max(1, (int)Math.Round(el.WidthFraction * ContentWidth));

            switch ((el.ElementType ?? "text").ToLowerInvariant())
            {
                case "image":
                    items.Add(new LayoutItem { Type = "Image", Left = left, Top = y, Width = width, Height = 30, Stretch = true });
                    y += 34;
                    break;
                case "signature_line":
                    items.Add(MakeHLine(left, y + 10, width));
                    y += 14;
                    break;
                default:
                    items.Add(MakeLabel(el.Text ?? "", left, y, width, LabelHeight,
                        el.FontSize > 0 ? el.FontSize : 9, el.Bold, el.Align));
                    y += LabelHeight + 2;
                    break;
            }
        }
        return y;
    }

    // ═══════════════════════════════════════════
    //  계산 헬퍼
    // ═══════════════════════════════════════════

    private static int[] CalculateColumnBoundaries(List<ColumnDef> columns, int tableLeft, int tableWidth)
    {
        var totalFraction = columns.Sum(c => c.WidthFraction);
        var normFactor = totalFraction > 0 ? 1.0 / totalFraction : 1.0;

        var bounds = new int[columns.Count + 1];
        bounds[0] = tableLeft;

        var accum = 0.0;
        for (var i = 0; i < columns.Count; i++)
        {
            accum += columns[i].WidthFraction * normFactor;
            bounds[i + 1] = tableLeft + (int)Math.Round(accum * tableWidth);
        }
        bounds[^1] = tableLeft + tableWidth; // 마지막 열은 정확하게

        return bounds;
    }

    private static List<int> CalculateRowYPositions(List<TableRow> rows, int startY)
    {
        var positions = new List<int> { startY };
        foreach (var row in rows)
            positions.Add(positions[^1] + ResolveRowHeight(row));
        return positions;
    }

    private static int ResolveRowHeight(TableRow row) =>
        (row.HeightHint ?? "standard").Trim().ToLowerInvariant() switch
        {
            "standard" => StandardRowHeight,
            "compact" => CompactRowHeight,
            "tall" => TallRowHeight,
            _ when int.TryParse(row.HeightHint, out var h) => Math.Clamp(h, 10, 300),
            _ => StandardRowHeight
        };

    // ═══════════════════════════════════════════
    //  텍스트 / 컴포넌트 이름 결정
    // ═══════════════════════════════════════════

    /// <summary>셀의 표시 텍스트를 결정. 내부 컴포넌트 식별자는 캡션으로 노출하지 않는다.</summary>
    private static string ResolveDisplayText(string? text, string? fieldName)
    {
        var visible = (text ?? "").Trim();
        if (!string.IsNullOrWhiteSpace(visible)) return visible;

        var field = (fieldName ?? "").Trim();
        if (string.IsNullOrWhiteSpace(field)) return string.Empty;

        return InternalNameRegex.IsMatch(field) ? string.Empty : field;
    }

    /// <summary>Delphi 컴포넌트 이름을 fieldName이나 표시 텍스트에서 유추.</summary>
    private static string? ResolveComponentName(string? displayText, string? fieldName)
    {
        var field = (fieldName ?? "").Trim();
        if (!string.IsNullOrWhiteSpace(field))
        {
            var match = FieldCodeRegex.Match(field);
            if (match.Success)
                return $"Qlb3_{match.Groups[1].Value.ToUpperInvariant()}";
        }

        var visible = (displayText ?? "").Trim();
        if (!string.IsNullOrWhiteSpace(visible))
        {
            var sanitized = SanitizeIdentifier(visible);
            if (!string.IsNullOrWhiteSpace(sanitized))
                return $"Qlb3_{sanitized}";
        }

        return null;
    }

    private static string SanitizeIdentifier(string raw)
    {
        var chars = raw
            .Select(ch => (ch is >= 'A' and <= 'Z') || (ch is >= 'a' and <= 'z') || (ch is >= '0' and <= '9') ? ch : '_')
            .ToArray();
        var candidate = new string(chars).Trim('_');
        if (string.IsNullOrWhiteSpace(candidate)) return string.Empty;

        while (candidate.Contains("__", StringComparison.Ordinal))
            candidate = candidate.Replace("__", "_", StringComparison.Ordinal);
        if (!char.IsLetter(candidate[0]) && candidate[0] != '_')
            candidate = $"N_{candidate}";

        return candidate.Length > 40 ? candidate[..40] : candidate;
    }

    // ═══════════════════════════════════════════
    //  LayoutItem 팩토리
    // ═══════════════════════════════════════════

    private static LayoutItem MakeLabel(string caption, int left, int top, int width, int height,
        int fontSize = 9, bool bold = false, string align = "Left", string? textColor = null, string? name = null) => new()
    {
        Name = name, Type = "Text",
        Left = Math.Max(0, left), Top = Math.Max(0, top),
        Width = Math.Max(1, width), Height = Math.Max(1, height),
        Caption = caption, FontSize = Math.Clamp(fontSize, 6, 24),
        Bold = bold, Align = align, Transparent = true,
        TextColor = textColor ?? "#000000"
    };

    private static LayoutItem MakeHLine(int left, int top, int width) => new()
    {
        Type = "Line", Left = Math.Max(0, left), Top = Math.Max(0, top),
        Width = Math.Max(1, width), Height = 1,
        Orientation = "H", Thickness = 1, StrokeColor = BorderColor
    };

    private static LayoutItem MakeVLine(int left, int top, int height) => new()
    {
        Type = "Line", Left = Math.Max(0, left), Top = Math.Max(0, top),
        Width = 1, Height = Math.Max(1, height),
        Orientation = "V", Thickness = 1, StrokeColor = BorderColor
    };

    private static LayoutItem MakeRect(int left, int top, int width, int height, string fillColor) => new()
    {
        Type = "Rect", Left = Math.Max(0, left), Top = Math.Max(0, top),
        Width = Math.Max(1, width), Height = Math.Max(1, height),
        FillColor = fillColor, Filled = true, StrokeColor = fillColor, Thickness = 1
    };
}
