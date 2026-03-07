namespace AutoEmply.Models;

/// <summary>
/// 폼 이미지에서 추출한 논리적 구조 (Phase 1 결과).
/// 픽셀 좌표 대신 비율(fraction)과 의미 힌트를 사용한다.
/// StructureToLayoutConverter가 이 구조를 LayoutSpec으로 변환한다.
/// </summary>
public sealed class FormStructure
{
    public string? Title { get; set; }
    public int TitleFontSize { get; set; } = 20;
    public List<FormSection> Sections { get; set; } = [];
    public List<FooterElement>? Footer { get; set; }
}

/// <summary>
/// 폼의 한 구역. SectionType에 따라 Table 또는 Elements를 사용한다.
/// </summary>
public sealed class FormSection
{
    /// <summary>"table" | "freeform" | "keyvalue"</summary>
    public string SectionType { get; set; } = "table";
    public string? Label { get; set; }
    public bool HasHeaderBackground { get; set; } = true;
    public TableDef? Table { get; set; }
    public List<FreeformElement>? Elements { get; set; }
}

// ─────────────────────────────────────────────
//  테이블 관련 모델
// ─────────────────────────────────────────────

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
    /// <summary>테이블 너비 대비 비율 (0.0~1.0). 모든 열의 합 = 1.0</summary>
    public double WidthFraction { get; set; }
    public string? Header { get; set; }
    public bool IsHeaderColumn { get; set; }
}

public sealed class TableRow
{
    /// <summary>"standard"(20px) | "compact"(14px) | "tall"(30px) | 정수 문자열</summary>
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

// ─────────────────────────────────────────────
//  자유 배치 / 푸터 요소
// ─────────────────────────────────────────────

public sealed class FreeformElement
{
    /// <summary>"text" | "checkbox" | "image" | "line"</summary>
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
    /// <summary>"text" | "image" | "signature_line"</summary>
    public string ElementType { get; set; } = "text";
    public string? Text { get; set; }
    public string Align { get; set; } = "Left";
    public bool Bold { get; set; }
    public int FontSize { get; set; } = 9;
    public double XFraction { get; set; }
    public double WidthFraction { get; set; } = 0.5;
}
