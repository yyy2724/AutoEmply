namespace AutoEmply.Models;

public sealed class ExportRequest
{
    public string FormName { get; set; } = string.Empty;
    public LayoutSpec? LayoutSpec { get; set; }
}
