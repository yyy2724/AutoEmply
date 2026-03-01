using AutoEmply.Models;

namespace AutoEmply.Services;

public sealed class LayoutPostProcessor
{
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
        var items = input.Items.Select(CloneItem).ToList();

        if (opts.SnapToGrid)
            items = SnapToGridPass(items);

        if (opts.AlignEdges)
            items = AlignEdgesPass(items);

        if (opts.NormalizeRowHeights)
            items = NormalizeRowHeightsPass(items);

        if (opts.CompleteBorders)
            items = CompleteBordersPass(items);

        if (opts.NormalizeLineGrid)
            items = NormalizeLineGridPass(items);

        if (opts.EnforceConsistency)
            items = EnforceConsistencyPass(items);

        if (opts.EnforceCanvasBounds)
            items = EnforceCanvasBoundsPass(items);

        if (opts.RemoveDuplicateBorders)
            items = RemoveDuplicateBordersPass(items);

        if (opts.SortByZOrder)
            items = SortByZOrder(items);

        return new LayoutSpec { Items = items, Pas = input.Pas };
    }

    /// <summary>
    /// Stage 1: Snap coordinates to a frequency-based grid.
    /// Finds commonly used X/Y positions and snaps nearby elements to them.
    /// </summary>
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

        // Grid = positions used by 2+ elements
        var xGrid = xFreq.Where(kv => kv.Value >= 2).Select(kv => kv.Key).OrderBy(x => x).ToList();
        var yGrid = yFreq.Where(kv => kv.Value >= 2).Select(kv => kv.Key).OrderBy(y => y).ToList();

        foreach (var item in items)
        {
            var origLeft = item.Left;
            item.Left = SnapToNearest(item.Left, xGrid, SnapThreshold);
            var right = origLeft + item.Width;
            var snappedRight = SnapToNearest(right, xGrid, SnapThreshold);
            item.Width = Math.Max(1, snappedRight - item.Left);

            var origTop = item.Top;
            item.Top = SnapToNearest(item.Top, yGrid, SnapThreshold);
            var bottom = origTop + item.Height;
            var snappedBottom = SnapToNearest(bottom, yGrid, SnapThreshold);
            item.Height = Math.Max(1, snappedBottom - item.Top);
        }

        return items;
    }

    /// <summary>
    /// Stage 2: Align edges. Elements at similar Y (or X) positions
    /// within threshold are unified to the most common coordinate.
    /// </summary>
    private List<LayoutItem> AlignEdgesPass(List<LayoutItem> items)
    {
        // Align Y positions (row alignment)
        AlignCoordinate(items, i => i.Top, (i, v) => i.Top = v, AlignThreshold);

        // Align X positions (column alignment)
        AlignCoordinate(items, i => i.Left, (i, v) => i.Left = v, AlignThreshold);

        // Align right edges
        AlignCoordinate(items, i => i.Left + i.Width,
            (i, v) => i.Width = Math.Max(1, v - i.Left), AlignThreshold);

        // Align bottom edges
        AlignCoordinate(items, i => i.Top + i.Height,
            (i, v) => i.Height = Math.Max(1, v - i.Top), AlignThreshold);

        return items;
    }

    /// <summary>
    /// Stage 3: Normalize standard sizes.
    /// Labels height 11-15 → 13, HLines height → 1, VLines width → 1.
    /// </summary>
    private static List<LayoutItem> NormalizeRowHeightsPass(List<LayoutItem> items)
    {
        foreach (var item in items)
        {
            if (IsText(item) && item.Height >= 11 && item.Height <= 15)
                item.Height = StandardLabelHeight;

            if (IsHLine(item))
                item.Height = 1;

            if (IsVLine(item))
                item.Width = 1;
        }

        return items;
    }

    /// <summary>
    /// Stage 4: Detect table regions and add missing border segments.
    /// Groups H-lines into separate table regions, ensures each has complete borders,
    /// and extends short V-lines to span their table region.
    /// </summary>
    private static List<LayoutItem> CompleteBordersPass(List<LayoutItem> items)
    {
        // Find horizontal lines spanning >40% of content width (tables)
        var wideHLines = items.Where(i => IsHLine(i) && i.Width > ContentWidth * 0.4)
            .OrderBy(i => i.Top).ToList();
        if (wideHLines.Count < 2) return items;

        // Group H-lines into table regions by Y-proximity (gap > 24px = new table)
        var tableRegions = new List<(int Top, int Bottom, int Left, int Right)>();
        var regionStart = wideHLines[0];
        var regionBottom = regionStart.Top;
        var regionLeft = regionStart.Left;
        var regionRight = regionStart.Left + regionStart.Width;

        for (var i = 1; i < wideHLines.Count; i++)
        {
            var line = wideHLines[i];
            if (line.Top - regionBottom > 24)
            {
                // New table region
                if (regionBottom > regionStart.Top)
                    tableRegions.Add((regionStart.Top, regionBottom, regionLeft, regionRight));
                regionStart = line;
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

        if (regionBottom > regionStart.Top)
            tableRegions.Add((regionStart.Top, regionBottom, regionLeft, regionRight));

        // For each table region, ensure left/right borders and extend short V-lines
        foreach (var (top, bottom, left, right) in tableRegions)
        {
            var height = bottom - top;
            if (height <= 0) continue;

            var vLines = items.Where(i => IsVLine(i) &&
                i.Top >= top - 4 && i.Top <= bottom + 4 &&
                i.Left >= left - 4 && i.Left <= right + 4).ToList();

            // Extend short V-lines only when they are already close to full span.
            // This avoids creating giant vertical strokes across unrelated sections.
            foreach (var vl in vLines)
            {
                var lineBottom = vl.Top + vl.Height;
                var nearTop = Math.Abs(vl.Top - top) <= 6;
                var nearBottom = Math.Abs(lineBottom - bottom) <= 6;
                var likelySameRegion = nearTop || nearBottom;
                if (likelySameRegion && vl.Height >= (int)(height * 0.6) && vl.Height < height - 4)
                {
                    vl.Top = top;
                    vl.Height = height;
                }
            }

            var existingXs = vLines.Select(i => i.Left).ToHashSet();

            // Ensure left border
            if (!existingXs.Any(x => Math.Abs(x - left) <= 3))
            {
                items.Add(new LayoutItem
                {
                    Type = "Line", Left = left, Top = top, Width = 1, Height = height,
                    Orientation = "V", Thickness = 1, StrokeColor = DefaultBorderColor
                });
            }

            // Ensure right border
            if (!existingXs.Any(x => Math.Abs(x - right) <= 3))
            {
                items.Add(new LayoutItem
                {
                    Type = "Line", Left = right, Top = top, Width = 1, Height = height,
                    Orientation = "V", Thickness = 1, StrokeColor = DefaultBorderColor
                });
            }
        }

        return items;
    }

    /// <summary>
    /// Stage 5: Enforce consistency rules.
    /// </summary>
    private static List<LayoutItem> EnforceConsistencyPass(List<LayoutItem> items)
    {
        foreach (var item in items)
        {
            // All text labels should be transparent
            if (IsText(item))
                item.Transparent = true;

            // Default font size
            if (IsText(item) && !item.FontSize.HasValue)
                item.FontSize = 9;

            // Clamp font sizes
            if (item.FontSize.HasValue)
                item.FontSize = Math.Clamp(item.FontSize.Value, 6, 24);

            // Clamp thickness
            if (item.Thickness.HasValue)
                item.Thickness = Math.Clamp(item.Thickness.Value, 1, 6);

            // Default border color for lines
            if (IsLine(item) && string.IsNullOrWhiteSpace(item.StrokeColor))
                item.StrokeColor = DefaultBorderColor;

            // Ensure non-negative coordinates
            item.Left = Math.Max(0, item.Left);
            item.Top = Math.Max(0, item.Top);
            item.Width = Math.Max(1, item.Width);
            item.Height = Math.Max(1, item.Height);
        }

        return items;
    }


    private static List<LayoutItem> NormalizeLineGridPass(List<LayoutItem> items)
    {
        var vLines = items.Where(IsVLine).ToList();
        var hLines = items.Where(IsHLine).ToList();
        if (vLines.Count == 0 || hLines.Count == 0)
        {
            return items;
        }

        var xGrid = vLines
            .Select(x => x.Left)
            .Distinct()
            .OrderBy(x => x)
            .ToList();
        var yGrid = hLines
            .Select(y => y.Top)
            .Distinct()
            .OrderBy(y => y)
            .ToList();

        foreach (var h in hLines)
        {
            h.Top = SnapToNearest(h.Top, yGrid, 4);
            var right = h.Left + h.Width;
            h.Left = SnapToNearest(h.Left, xGrid, 8);
            var snappedRight = SnapToNearest(right, xGrid, 8);
            h.Width = Math.Max(1, snappedRight - h.Left);
            h.Height = 1;
            h.Orientation = "H";
        }

        foreach (var v in vLines)
        {
            v.Left = SnapToNearest(v.Left, xGrid, 4);
            var bottom = v.Top + v.Height;
            v.Top = SnapToNearest(v.Top, yGrid, 8);
            var snappedBottom = SnapToNearest(bottom, yGrid, 8);
            v.Height = Math.Max(1, snappedBottom - v.Top);
            v.Width = 1;
            v.Orientation = "V";
        }

        return items;
    }

    private static List<LayoutItem> EnforceCanvasBoundsPass(List<LayoutItem> items)
    {
        foreach (var item in items)
        {
            item.Left = Math.Clamp(item.Left, CanvasLeft, CanvasRight - 1);
            item.Top = Math.Clamp(item.Top, CanvasTop, CanvasBottom - 1);

            if (item.Width < 1) item.Width = 1;
            if (item.Height < 1) item.Height = 1;

            var maxWidth = Math.Max(1, CanvasRight - item.Left);
            var maxHeight = Math.Max(1, CanvasBottom - item.Top);
            item.Width = Math.Min(item.Width, maxWidth);
            item.Height = Math.Min(item.Height, maxHeight);

            // Keep line semantics after clamping.
            if (IsHLine(item))
            {
                item.Height = 1;
            }
            else if (IsVLine(item))
            {
                item.Width = 1;
            }
        }

        return items;
    }

    private static List<LayoutItem> RemoveDuplicateBordersPass(List<LayoutItem> items)
    {
        var output = new List<LayoutItem>(items.Count);
        var lineGroups = items
            .Where(IsLine)
            .GroupBy(i => $"{NormalizeOrientation(i)}|{i.Left}|{i.Top}|{i.Width}|{i.Height}");

        var chosenLines = new HashSet<LayoutItem>();
        foreach (var group in lineGroups)
        {
            var chosen = group
                .OrderBy(i => i.Thickness.GetValueOrDefault(1))
                .ThenBy(i => string.IsNullOrWhiteSpace(i.StrokeColor) ? 1 : 0)
                .First();
            chosenLines.Add(chosen);
        }

        foreach (var item in items)
        {
            if (!IsLine(item))
            {
                output.Add(item);
                continue;
            }

            if (chosenLines.Contains(item))
            {
                output.Add(item);
            }
        }

        return output;
    }

    /// <summary>
    /// Stage 6: Sort by Z-order. Rect (background) → Line (border) → Image → Text (top).
    /// </summary>
    private static List<LayoutItem> SortByZOrder(List<LayoutItem> items)
    {
        return items
            .OrderBy(i => ZOrderPriority(i.Type))
            .ThenBy(i => i.Top)
            .ThenBy(i => i.Left)
            .ToList();
    }

    private static int ZOrderPriority(string? type) => (type ?? string.Empty).ToLowerInvariant() switch
    {
        "rect" => 0,
        "line" => 1,
        "image" => 2,
        "text" => 3,
        _ => 4
    };

    // --- Helpers ---

    private static void AlignCoordinate(List<LayoutItem> items,
        Func<LayoutItem, int> getCoord, Action<LayoutItem, int> setCoord, int threshold)
    {
        var groups = GroupByProximity(items, getCoord, threshold);
        foreach (var group in groups)
        {
            if (group.Count < 2) continue;

            // Use the most common value (mode) as canonical
            var canonical = group
                .GroupBy(i => getCoord(i))
                .OrderByDescending(g => g.Count())
                .First().Key;

            foreach (var item in group)
                setCoord(item, canonical);
        }
    }

    private static List<List<LayoutItem>> GroupByProximity(
        List<LayoutItem> items, Func<LayoutItem, int> getCoord, int threshold)
    {
        var sorted = items.OrderBy(getCoord).ToList();
        var groups = new List<List<LayoutItem>>();

        if (sorted.Count == 0) return groups;

        var currentGroup = new List<LayoutItem> { sorted[0] };
        for (var i = 1; i < sorted.Count; i++)
        {
            if (Math.Abs(getCoord(sorted[i]) - getCoord(sorted[i - 1])) <= threshold)
            {
                currentGroup.Add(sorted[i]);
            }
            else
            {
                groups.Add(currentGroup);
                currentGroup = [sorted[i]];
            }
        }

        groups.Add(currentGroup);
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
            if (dist < minDist)
            {
                minDist = dist;
                closest = g;
            }

            if (dist == 0) break;
            if (g > value + threshold) break; // sorted, no need to check further
        }

        return minDist <= threshold ? closest : value;
    }

    private static void Increment(Dictionary<int, int> freq, int key)
    {
        freq[key] = freq.GetValueOrDefault(key) + 1;
    }

    private static bool IsText(LayoutItem item) =>
        string.Equals(item.Type, "Text", StringComparison.OrdinalIgnoreCase);

    private static bool IsLine(LayoutItem item) =>
        string.Equals(item.Type, "Line", StringComparison.OrdinalIgnoreCase);

    private static bool IsHLine(LayoutItem item) =>
        IsLine(item) && (string.Equals(item.Orientation, "H", StringComparison.OrdinalIgnoreCase) || item.Width > item.Height);

    private static bool IsVLine(LayoutItem item) =>
        IsLine(item) && (string.Equals(item.Orientation, "V", StringComparison.OrdinalIgnoreCase) || item.Height > item.Width);

    private static string NormalizeOrientation(LayoutItem item)
    {
        if (IsHLine(item)) return "H";
        if (IsVLine(item)) return "V";
        return (item.Orientation ?? string.Empty).Trim().ToUpperInvariant();
    }

    private static LayoutItem CloneItem(LayoutItem source)
    {
        return new LayoutItem
        {
            Name = source.Name,
            Type = source.Type,
            Left = source.Left,
            Top = source.Top,
            Width = source.Width,
            Height = source.Height,
            Caption = source.Caption,
            Align = source.Align,
            FontSize = source.FontSize,
            Bold = source.Bold,
            Transparent = source.Transparent,
            TextColor = source.TextColor,
            Orientation = source.Orientation,
            Thickness = source.Thickness,
            StrokeColor = source.StrokeColor,
            FillColor = source.FillColor,
            Filled = source.Filled,
            Stretch = source.Stretch
        };
    }
}

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
