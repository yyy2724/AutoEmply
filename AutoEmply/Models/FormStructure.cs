namespace AutoEmply.Models;

/// <summary>
/// Root structure describing a form's logical layout as extracted by Claude.
/// No pixel coordinates - uses fractions and semantic hints.
/// </summary>
public sealed class FormStructure
{
    public string? Title { get; set; }
    public int TitleFontSize { get; set; } = 20;
    public List<FormSection> Sections { get; set; } = [];
    public List<FooterElement>? Footer { get; set; }
}

public sealed class FormSection
{
    /// <summary>"table", "freeform", "keyvalue"</summary>
    public string SectionType { get; set; } = "table";
    public string? Label { get; set; }
    public bool HasHeaderBackground { get; set; } = true;
    public TableDef? Table { get; set; }
    public List<FreeformElement>? Elements { get; set; }
}

public sealed class TableDef
{
    public List<ColumnDef> Columns { get; set; } = [];
    public List<TableRow> Rows { get; set; } = [];
    public bool FullWidth { get; set; } = true;
    public double LeftFraction { get; set; }
    public double WidthFraction { get; set; } = 1.0;
}

public sealed class ColumnDef
{
    /// <summary>Fraction of table width (0.0~1.0). All columns must sum to 1.0.</summary>
    public double WidthFraction { get; set; }
    public string? Header { get; set; }
    public bool IsHeaderColumn { get; set; }
}

public sealed class TableRow
{
    /// <summary>"standard"(20px), "compact"(14px), "tall"(30px), or integer string.</summary>
    public string HeightHint { get; set; } = "standard";
    public List<TableCell> Cells { get; set; } = [];
    public bool IsHeaderRow { get; set; }
}

public sealed class TableCell
{
    public string Text { get; set; } = string.Empty;
    public string Align { get; set; } = "Left";
    public bool Bold { get; set; }
    public int FontSize { get; set; } = 9;
    public int ColSpan { get; set; } = 1;
    public int RowSpan { get; set; } = 1;
    public bool HasBackground { get; set; }
    public string? FieldName { get; set; }
    public string? TextColor { get; set; }
}

public sealed class FreeformElement
{
    /// <summary>"text", "checkbox", "image", "line"</summary>
    public string ElementType { get; set; } = "text";
    public string? Text { get; set; }
    public double XFraction { get; set; }
    public double YFraction { get; set; }
    public double WidthFraction { get; set; } = 0.1;
    public double HeightFraction { get; set; } = 0.02;
    public string Align { get; set; } = "Left";
    public bool Bold { get; set; }
    public int FontSize { get; set; } = 9;
}

public sealed class FooterElement
{
    /// <summary>"text", "image", "signature_line"</summary>
    public string ElementType { get; set; } = "text";
    public string? Text { get; set; }
    public string Align { get; set; } = "Left";
    public bool Bold { get; set; }
    public int FontSize { get; set; } = 9;
    public double XFraction { get; set; }
    public double WidthFraction { get; set; } = 0.5;
}
