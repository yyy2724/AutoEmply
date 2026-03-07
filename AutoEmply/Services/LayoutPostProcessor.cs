using AutoEmply.Models;

namespace AutoEmply.Services;

/// <summary>
/// Claude가 생성한 LayoutSpec을 후처리하여 품질을 높이는 다단계 파이프라인.
///
/// 처리 순서 (각 단계는 독립적으로 켜고 끌 수 있다):
///   1. SnapToGrid       - 빈도 기반 격자에 좌표 스냅
///   2. AlignEdges       - 인접 요소의 Top/Left/Right/Bottom 정렬 통일
///   3. NormalizeRows    - 라벨 높이 13px, 수평선 높이 1px 등 표준화
///   4. CompleteBorders  - 테이블 영역의 누락된 세로 테두리 보완
///   5. NormalizeLineGrid- 선 좌표를 격자에 맞춤
///   6. Consistency      - 투명도, 기본 색상 등 일관성 규칙 적용
///   7. CanvasBounds     - 캔버스 경계 안으로 클램핑
///   8. RemoveDuplicates - 동일 위치의 중복 선 제거
///   9. ZOrder           - Rect → Line → Image → Text 순으로 정렬
/// </summary>
public sealed class LayoutPostProcessor
{
    // ── 캔버스/임계값 상수 ──
    private const int ContentWidth = 774;
    private const int CanvasLeft = 10;
    private const int CanvasRight = 784;
    private const int CanvasTop = 0;
    private const int CanvasBottom = 1600;
    private const int SnapThreshold = 4;
    private const int AlignThreshold = 3;
    private const int StandardLabelHeight = 13;
    private const string DefaultBorderColor = "#000000";

    public LayoutSpec Process(LayoutSpec input, PostProcessingOptions? options = null)
    {
        var opts = options ?? PostProcessingOptions.Default;
        var items = input.Items.Select(i => i.Clone()).ToList();

        if (opts.SnapToGrid)          items = SnapToGridPass(items);
        if (opts.AlignEdges)          items = AlignEdgesPass(items);
        if (opts.NormalizeRowHeights) items = NormalizeRowHeightsPass(items);
        if (opts.CompleteBorders)     items = CompleteBordersPass(items);
        if (opts.NormalizeLineGrid)   items = NormalizeLineGridPass(items);
        if (opts.EnforceConsistency)  items = EnforceConsistencyPass(items);
        if (opts.EnforceCanvasBounds) items = EnforceCanvasBoundsPass(items);
        if (opts.RemoveDuplicateBorders) items = RemoveDuplicateBordersPass(items);
        if (opts.SortByZOrder)        items = SortByZOrder(items);

        return new LayoutSpec { Items = items, Pas = input.Pas };
    }

    // ═══════════════════════════════════════════
    //  Stage 1: 격자 스냅
    // ═══════════════════════════════════════════

    private List<LayoutItem> SnapToGridPass(List<LayoutItem> items)
    {
        var xFreq = new Dictionary<int, int>();
        var yFreq = new Dictionary<int, int>();

        foreach (var item in items)
        {
            Increment(xFreq, item.Left);
            Increment(xFreq, item.Left + item.Width);
            Increment(yFreq, item.Top);
            Increment(yFreq, item.Top + item.Height);
        }

        // 2회 이상 사용된 좌표 = 격자선
        var xGrid = xFreq.Where(kv => kv.Value >= 2).Select(kv => kv.Key).OrderBy(x => x).ToList();
        var yGrid = yFreq.Where(kv => kv.Value >= 2).Select(kv => kv.Key).OrderBy(y => y).ToList();

        foreach (var item in items)
        {
            var origLeft = item.Left;
            item.Left = SnapToNearest(item.Left, xGrid, SnapThreshold);
            item.Width = Math.Max(1, SnapToNearest(origLeft + item.Width, xGrid, SnapThreshold) - item.Left);

            var origTop = item.Top;
            item.Top = SnapToNearest(item.Top, yGrid, SnapThreshold);
            item.Height = Math.Max(1, SnapToNearest(origTop + item.Height, yGrid, SnapThreshold) - item.Top);
        }

        return items;
    }

    // ═══════════════════════════════════════════
    //  Stage 2: 엣지 정렬
    // ═══════════════════════════════════════════

    private static List<LayoutItem> AlignEdgesPass(List<LayoutItem> items)
    {
        AlignCoordinate(items, i => i.Top, (i, v) => i.Top = v, AlignThreshold);
        AlignCoordinate(items, i => i.Left, (i, v) => i.Left = v, AlignThreshold);
        AlignCoordinate(items, i => i.Left + i.Width, (i, v) => i.Width = Math.Max(1, v - i.Left), AlignThreshold);
        AlignCoordinate(items, i => i.Top + i.Height, (i, v) => i.Height = Math.Max(1, v - i.Top), AlignThreshold);
        return items;
    }

    // ═══════════════════════════════════════════
    //  Stage 3: 높이/너비 표준화
    // ═══════════════════════════════════════════

    private static List<LayoutItem> NormalizeRowHeightsPass(List<LayoutItem> items)
    {
        foreach (var item in items)
        {
            if (IsText(item) && item.Height is >= 11 and <= 15)
                item.Height = StandardLabelHeight;
            if (IsHLine(item)) item.Height = 1;
            if (IsVLine(item)) item.Width = 1;
        }
        return items;
    }

    // ═══════════════════════════════════════════
    //  Stage 4: 누락 테두리 보완
    // ═══════════════════════════════════════════

    private static List<LayoutItem> CompleteBordersPass(List<LayoutItem> items)
    {
        // 테이블 가로선 = 전체 너비의 40% 이상인 수평선
        var wideHLines = items
            .Where(i => IsHLine(i) && i.Width > ContentWidth * 0.4)
            .OrderBy(i => i.Top).ToList();
        if (wideHLines.Count < 2) return items;

        // Y 간격이 24px 초과이면 별도 테이블 영역으로 분리
        var regions = DetectTableRegions(wideHLines);

        foreach (var (top, bottom, left, right) in regions)
        {
            var height = bottom - top;
            if (height <= 0) continue;

            // 영역 내 세로선 찾기 및 확장
            var vLines = items.Where(i =>
                IsVLine(i) &&
                i.Top >= top - 4 && i.Top <= bottom + 4 &&
                i.Left >= left - 4 && i.Left <= right + 4).ToList();

            foreach (var vl in vLines)
            {
                var nearTop = Math.Abs(vl.Top - top) <= 6;
                var nearBottom = Math.Abs(vl.Top + vl.Height - bottom) <= 6;
                if ((nearTop || nearBottom) && vl.Height >= (int)(height * 0.6) && vl.Height < height - 4)
                {
                    vl.Top = top;
                    vl.Height = height;
                }
            }

            var existingXs = vLines.Select(i => i.Left).ToHashSet();

            // 좌측 테두리 보완
            if (!existingXs.Any(x => Math.Abs(x - left) <= 3))
                items.Add(MakeBorderVLine(left, top, height));

            // 우측 테두리 보완
            if (!existingXs.Any(x => Math.Abs(x - right) <= 3))
                items.Add(MakeBorderVLine(right, top, height));
        }

        return items;
    }

    private static List<(int Top, int Bottom, int Left, int Right)> DetectTableRegions(List<LayoutItem> sortedHLines)
    {
        var regions = new List<(int, int, int, int)>();
        var start = sortedHLines[0];
        var regionBottom = start.Top;
        var regionLeft = start.Left;
        var regionRight = start.Left + start.Width;

        for (var i = 1; i < sortedHLines.Count; i++)
        {
            var line = sortedHLines[i];
            if (line.Top - regionBottom > 24)
            {
                if (regionBottom > start.Top)
                    regions.Add((start.Top, regionBottom, regionLeft, regionRight));
                start = line;
                regionBottom = line.Top;
                regionLeft = line.Left;
                regionRight = line.Left + line.Width;
            }
            else
            {
                regionBottom = line.Top;
                regionLeft = Math.Min(regionLeft, line.Left);
                regionRight = Math.Max(regionRight, line.Left + line.Width);
            }
        }

        if (regionBottom > start.Top)
            regions.Add((start.Top, regionBottom, regionLeft, regionRight));

        return regions;
    }

    // ═══════════════════════════════════════════
    //  Stage 5: 선 격자 정규화
    // ═══════════════════════════════════════════

    private static List<LayoutItem> NormalizeLineGridPass(List<LayoutItem> items)
    {
        var vLines = items.Where(IsVLine).ToList();
        var hLines = items.Where(IsHLine).ToList();
        if (vLines.Count == 0 || hLines.Count == 0) return items;

        var xGrid = vLines.Select(v => v.Left).Distinct().OrderBy(x => x).ToList();
        var yGrid = hLines.Select(h => h.Top).Distinct().OrderBy(y => y).ToList();

        foreach (var h in hLines)
        {
            h.Top = SnapToNearest(h.Top, yGrid, 4);
            var right = h.Left + h.Width;
            h.Left = SnapToNearest(h.Left, xGrid, 8);
            h.Width = Math.Max(1, SnapToNearest(right, xGrid, 8) - h.Left);
            h.Height = 1;
            h.Orientation = "H";
        }

        foreach (var v in vLines)
        {
            v.Left = SnapToNearest(v.Left, xGrid, 4);
            var bottom = v.Top + v.Height;
            v.Top = SnapToNearest(v.Top, yGrid, 8);
            v.Height = Math.Max(1, SnapToNearest(bottom, yGrid, 8) - v.Top);
            v.Width = 1;
            v.Orientation = "V";
        }

        return items;
    }

    // ═══════════════════════════════════════════
    //  Stage 6: 일관성 규칙
    // ═══════════════════════════════════════════

    private static List<LayoutItem> EnforceConsistencyPass(List<LayoutItem> items)
    {
        foreach (var item in items)
        {
            if (IsText(item)) item.Transparent = true;
            if (IsText(item) && !item.FontSize.HasValue) item.FontSize = 9;
            if (item.FontSize.HasValue) item.FontSize = Math.Clamp(item.FontSize.Value, 6, 24);
            if (item.Thickness.HasValue) item.Thickness = Math.Clamp(item.Thickness.Value, 1, 6);
            if (IsLine(item) && string.IsNullOrWhiteSpace(item.StrokeColor)) item.StrokeColor = DefaultBorderColor;
            item.Left = Math.Max(0, item.Left);
            item.Top = Math.Max(0, item.Top);
            item.Width = Math.Max(1, item.Width);
            item.Height = Math.Max(1, item.Height);
        }
        return items;
    }

    // ═══════════════════════════════════════════
    //  Stage 7: 캔버스 경계 클램핑
    // ═══════════════════════════════════════════

    private static List<LayoutItem> EnforceCanvasBoundsPass(List<LayoutItem> items)
    {
        foreach (var item in items)
        {
            item.Left = Math.Clamp(item.Left, CanvasLeft, CanvasRight - 1);
            item.Top = Math.Clamp(item.Top, CanvasTop, CanvasBottom - 1);
            item.Width = Math.Clamp(item.Width, 1, CanvasRight - item.Left);
            item.Height = Math.Clamp(item.Height, 1, CanvasBottom - item.Top);

            if (IsHLine(item)) item.Height = 1;
            else if (IsVLine(item)) item.Width = 1;
        }
        return items;
    }

    // ═══════════════════════════════════════════
    //  Stage 8: 중복 선 제거
    // ═══════════════════════════════════════════

    private static List<LayoutItem> RemoveDuplicateBordersPass(List<LayoutItem> items)
    {
        var chosenLines = items
            .Where(IsLine)
            .GroupBy(i => $"{NormalizeOrientation(i)}|{i.Left}|{i.Top}|{i.Width}|{i.Height}")
            .Select(g => g.OrderBy(i => i.Thickness.GetValueOrDefault(1)).First())
            .ToHashSet();

        return items.Where(i => !IsLine(i) || chosenLines.Contains(i)).ToList();
    }

    // ═══════════════════════════════════════════
    //  Stage 9: Z-Order 정렬
    // ═══════════════════════════════════════════

    private static List<LayoutItem> SortByZOrder(List<LayoutItem> items) =>
        items.OrderBy(i => ZPriority(i.Type)).ThenBy(i => i.Top).ThenBy(i => i.Left).ToList();

    private static int ZPriority(string? type) => (type ?? "").ToLowerInvariant() switch
    {
        "rect" => 0, "line" => 1, "image" => 2, "text" => 3, _ => 4
    };

    // ═══════════════════════════════════════════
    //  공통 헬퍼
    // ═══════════════════════════════════════════

    private static void AlignCoordinate(
        List<LayoutItem> items, Func<LayoutItem, int> get, Action<LayoutItem, int> set, int threshold)
    {
        foreach (var group in GroupByProximity(items, get, threshold))
        {
            if (group.Count < 2) continue;
            var canonical = group.GroupBy(get).OrderByDescending(g => g.Count()).First().Key;
            foreach (var item in group) set(item, canonical);
        }
    }

    private static List<List<LayoutItem>> GroupByProximity(
        List<LayoutItem> items, Func<LayoutItem, int> get, int threshold)
    {
        var sorted = items.OrderBy(get).ToList();
        var groups = new List<List<LayoutItem>>();
        if (sorted.Count == 0) return groups;

        var current = new List<LayoutItem> { sorted[0] };
        for (var i = 1; i < sorted.Count; i++)
        {
            if (Math.Abs(get(sorted[i]) - get(sorted[i - 1])) <= threshold)
                current.Add(sorted[i]);
            else
            {
                groups.Add(current);
                current = [sorted[i]];
            }
        }
        groups.Add(current);
        return groups;
    }

    private static int SnapToNearest(int value, List<int> grid, int threshold)
    {
        if (grid.Count == 0) return value;

        var closest = value;
        var minDist = int.MaxValue;
        foreach (var g in grid)
        {
            var dist = Math.Abs(value - g);
            if (dist < minDist) { minDist = dist; closest = g; }
            if (dist == 0) break;
            if (g > value + threshold) break;
        }
        return minDist <= threshold ? closest : value;
    }

    private static void Increment(Dictionary<int, int> freq, int key) =>
        freq[key] = freq.GetValueOrDefault(key) + 1;

    private static LayoutItem MakeBorderVLine(int left, int top, int height) => new()
    {
        Type = "Line", Left = left, Top = top, Width = 1, Height = height,
        Orientation = "V", Thickness = 1, StrokeColor = DefaultBorderColor
    };

    // ── 타입 판별 ──
    private static bool IsText(LayoutItem i) => i.Type.Equals("Text", StringComparison.OrdinalIgnoreCase);
    private static bool IsLine(LayoutItem i) => i.Type.Equals("Line", StringComparison.OrdinalIgnoreCase);
    private static bool IsHLine(LayoutItem i) => IsLine(i) && (i.Orientation?.Equals("H", StringComparison.OrdinalIgnoreCase) == true || i.Width > i.Height);
    private static bool IsVLine(LayoutItem i) => IsLine(i) && (i.Orientation?.Equals("V", StringComparison.OrdinalIgnoreCase) == true || i.Height > i.Width);

    private static string NormalizeOrientation(LayoutItem i) =>
        IsHLine(i) ? "H" : IsVLine(i) ? "V" : (i.Orientation ?? "").Trim().ToUpperInvariant();
}

/// <summary>후처리 파이프라인의 각 단계를 개별적으로 켜고 끌 수 있는 옵션.</summary>
public sealed class PostProcessingOptions
{
    public bool SnapToGrid { get; set; } = true;
    public bool AlignEdges { get; set; } = true;
    public bool NormalizeRowHeights { get; set; } = true;
    public bool CompleteBorders { get; set; } = true;
    public bool NormalizeLineGrid { get; set; } = true;
    public bool EnforceConsistency { get; set; } = true;
    public bool EnforceCanvasBounds { get; set; } = true;
    public bool RemoveDuplicateBorders { get; set; } = true;
    public bool SortByZOrder { get; set; } = true;

    public static PostProcessingOptions Default => new();
}
