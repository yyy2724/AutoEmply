namespace AutoEmply.Models;

public sealed class LayoutSpec
{
    public List<LayoutItem> Items { get; set; } = [];
}

public sealed class LayoutItem
{
    public string Type { get; set; } = string.Empty;
    public int Left { get; set; }
    public int Top { get; set; }
    public int Width { get; set; }
    public int Height { get; set; }

    public string? Caption { get; set; }
    public string? Align { get; set; }
    public int? FontSize { get; set; }
    public bool? Bold { get; set; }
    public bool? Transparent { get; set; }

    public string? Orientation { get; set; }
    public int? Thickness { get; set; }

    public bool? Stretch { get; set; }
}
