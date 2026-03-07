namespace AutoEmply.Models;

/// <summary>
/// AI가 생성한 Delphi QuickReport 레이아웃 명세.
/// Items = 화면에 배치할 컴포넌트 목록, Pas = 생성할 파스칼 코드 정보.
/// </summary>
public sealed class LayoutSpec
{
    public List<LayoutItem> Items { get; set; } = [];
    public PasSpec? Pas { get; set; }
}

/// <summary>
/// QuickReport 위에 놓이는 단일 컴포넌트(텍스트, 선, 사각형, 이미지).
/// 좌표 단위는 픽셀이며, DelphiGenerator가 mm 단위로 변환한다.
/// </summary>
public sealed class LayoutItem
{
    public string? Name { get; set; }
    public string Type { get; set; } = string.Empty;
    public int Left { get; set; }
    public int Top { get; set; }
    public int Width { get; set; }
    public int Height { get; set; }

    // --- 텍스트 속성 ---
    public string? Caption { get; set; }
    public string? Align { get; set; }
    public int? FontSize { get; set; }
    public bool? Bold { get; set; }
    public bool? Transparent { get; set; }
    public string? TextColor { get; set; }

    // --- 선/도형 속성 ---
    public string? Orientation { get; set; }
    public int? Thickness { get; set; }
    public string? StrokeColor { get; set; }
    public string? FillColor { get; set; }
    public bool? Filled { get; set; }

    // --- 이미지 속성 ---
    public bool? Stretch { get; set; }

    /// <summary>
    /// 모든 속성을 복사한 새 인스턴스를 반환한다.
    /// PostProcessor, Generator 등 여러 곳에서 원본 훼손 없이 가공할 때 사용.
    /// </summary>
    public LayoutItem Clone() => new()
    {
        Name = Name,
        Type = Type,
        Left = Left,
        Top = Top,
        Width = Width,
        Height = Height,
        Caption = Caption,
        Align = Align,
        FontSize = FontSize,
        Bold = Bold,
        Transparent = Transparent,
        TextColor = TextColor,
        Orientation = Orientation,
        Thickness = Thickness,
        StrokeColor = StrokeColor,
        FillColor = FillColor,
        Filled = Filled,
        Stretch = Stretch
    };
}

/// <summary>
/// .pas 파일에 삽입할 uses 절과 메서드 목록.
/// </summary>
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
