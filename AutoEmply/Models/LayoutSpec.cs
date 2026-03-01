namespace AutoEmply.Models;

public sealed class LayoutSpec
{
    public List<LayoutItem> Items { get; set; } = [];
    public PasSpec? Pas { get; set; }
}

public sealed class LayoutItem
{
    public string? Name { get; set; }
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
    public string? TextColor { get; set; }

    public string? Orientation { get; set; }
    public int? Thickness { get; set; }
    public string? StrokeColor { get; set; }
    public string? FillColor { get; set; }
    public bool? Filled { get; set; }

    public bool? Stretch { get; set; }
}

public sealed class PasSpec
{
    public List<string>? Uses { get; set; }
    public List<PasMethodSpec>? Methods { get; set; }
}

public sealed class PasMethodSpec
{
    public string Declaration { get; set; } = string.Empty;
    public List<string>? Body { get; set; }
}
