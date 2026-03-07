using System.Globalization;
using System.IO.Compression;
using System.Text;
using AutoEmply.Models;

namespace AutoEmply.Services;

/// <summary>
/// LayoutSpec → Delphi QuickReport 파일(.dfm + .pas) 생성기.
///
/// 처리 흐름:
///   1. LayoutSpec.Items를 정규화(타입 매핑, 좌표 보정, 중복 제거)
///   2. DetailBand 크기를 아이템 범위에 맞게 계산
///   3. DFM(Delphi Form) 텍스트 생성
///   4. PAS(Pascal Unit) 텍스트 생성
///   5. ZIP으로 묶어 반환
/// </summary>
public sealed class DelphiGenerator
{
    // ── 상수 ──
    private const double PixelToMm = 2.645833333333333;
    private const int QuickRepWidth = 794;
    private const int MaxBandWidth = QuickRepWidth;
    private const int MaxBandHeight = 6000;
    private const int BandPadding = 2;
    private const int MinFontSize = 6;
    private const int MaxFontSize = 24;
    private const int MaxPenThickness = 6;

    // ═══════════════════════════════════════════
    //  공개 API
    // ═══════════════════════════════════════════

    public byte[] GenerateZip(string formName, LayoutSpec layoutSpec)
    {
        var className = formName.Replace("_", string.Empty, StringComparison.Ordinal);
        var dfm = GenerateDfm(formName, className, layoutSpec, out var components);
        var pas = GeneratePas(formName, className, components, layoutSpec.Pas);

        using var stream = new MemoryStream();
        using (var archive = new ZipArchive(stream, ZipArchiveMode.Create, leaveOpen: true))
        {
            WriteEntry(archive, $"{formName}.dfm", dfm);
            WriteEntry(archive, $"{formName}.pas", pas);
        }
        return stream.ToArray();
    }

    // ═══════════════════════════════════════════
    //  DFM 생성
    // ═══════════════════════════════════════════

    public string GenerateDfm(string formName, string className, LayoutSpec layoutSpec, out List<ComponentRef> components)
    {
        components = [new("QuickRep1", "TQuickRep"), new("DetailBand1", "TQRBand")];

        var items = PrepareItems(layoutSpec.Items);
        var bandSize = CalculateBandSize(items);
        items = ClampItemsToBand(items, bandSize.Width, bandSize.Height);

        var sb = new StringBuilder();
        WriteFormHeader(sb, formName, className);
        WriteQuickRepHeader(sb);
        WriteDetailBandHeader(sb, bandSize);
        WriteComponentItems(sb, items, components);
        WriteClosingTags(sb);

        return sb.ToString();
    }

    // ── DFM: 섹션별 출력 ──

    private void WriteFormHeader(StringBuilder sb, string formName, string className)
    {
        sb.AppendLine($"object {formName}: T{className}");
        sb.AppendLine("  Left = 0");
        sb.AppendLine("  Top = 0");
        sb.AppendLine("  Width = 800");
        sb.AppendLine("  Height = 600");
        sb.AppendLine($"  Caption = {EncodeDelphi(formName)}");
        sb.AppendLine("  OldCreateOrder = False");
        sb.AppendLine("  PixelsPerInch = 96");
        sb.AppendLine("  TextHeight = 13");
    }

    private static void WriteQuickRepHeader(StringBuilder sb)
    {
        sb.AppendLine("  object QuickRep1: TQuickRep");
        sb.AppendLine("    Left = 0");
        sb.AppendLine("    Top = 0");
        sb.AppendLine($"    Width = {QuickRepWidth}");
        sb.AppendLine("    Height = 1123");
        sb.AppendLine("    DataSet = nil");
        sb.AppendLine("    Functions.Strings = (");
        sb.AppendLine("      'PAGENUMBER'");
        sb.AppendLine("      'COLUMNNUMBER'");
        sb.AppendLine("      'REPORTTITLE'");
        sb.AppendLine("      'QRSTRINGSBAND1'");
        sb.AppendLine("      'QRSTRINGSBAND2'");
        sb.AppendLine("      'QRSTRINGSBAND3'");
        sb.AppendLine("      'QRSTRINGSBAND4'");
        sb.AppendLine("      'QRSTRINGSBAND5'");
        sb.AppendLine("      'QRSTRINGSBAND6'");
        sb.AppendLine("      'QRSTRINGSBAND7')");
        sb.AppendLine("    Functions.DATA = (");
        sb.AppendLine("      '0'");
        sb.AppendLine("      '0'");
        sb.AppendLine("      ''''''");
        sb.AppendLine("      ''");
        sb.AppendLine("      ''");
        sb.AppendLine("      ''");
        sb.AppendLine("      ''");
        sb.AppendLine("      ''");
        sb.AppendLine("      ''");
        sb.AppendLine("      '')");
        sb.AppendLine("    Options = [FirstPageHeader, LastPageFooter]");
        sb.AppendLine("    Page.Columns = 1");
        sb.AppendLine("    Page.Orientation = poPortrait");
        sb.AppendLine("    Page.PaperSize = A4");
        sb.AppendLine("    Page.Values = (");
        sb.AppendLine("      100.000000000000000000");
        sb.AppendLine("      2970.000000000000000000");
        sb.AppendLine("      100.000000000000000000");
        sb.AppendLine("      2100.000000000000000000");
        sb.AppendLine("      100.000000000000000000");
        sb.AppendLine("      100.000000000000000000");
        sb.AppendLine("      0.000000000000000000)");
        sb.AppendLine("    PrinterSettings.Copies = 1");
        sb.AppendLine("    PrinterSettings.OutputBin = Auto");
        sb.AppendLine("    PrinterSettings.Duplex = False");
        sb.AppendLine("    PrinterSettings.FirstPage = 0");
        sb.AppendLine("    PrinterSettings.LastPage = 0");
        sb.AppendLine("    PrinterSettings.UseStandardprinter = False");
        sb.AppendLine("    PrinterSettings.UseCustomBinCode = False");
    }

    private void WriteDetailBandHeader(StringBuilder sb, (int Width, int Height) size)
    {
        sb.AppendLine("    object DetailBand1: TQRBand");
        sb.AppendLine("      Left = 0");
        sb.AppendLine("      Top = 38");
        sb.AppendLine($"      Width = {size.Width}");
        sb.AppendLine($"      Height = {size.Height}");
        sb.AppendLine("      BandType = rbDetail");
        sb.AppendLine($"      {BandSizeValues(size.Height, size.Width).Replace(Environment.NewLine, Environment.NewLine + "      ", StringComparison.Ordinal)}");
    }

    private void WriteComponentItems(StringBuilder sb, IReadOnlyList<LayoutItem> items, List<ComponentRef> components)
    {
        var counters = new ComponentCounters();
        var usedNames = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var item in items)
        {
            var type = (item.Type ?? string.Empty).Trim();

            if (type.Equals("Text", StringComparison.OrdinalIgnoreCase))
                WriteLabel(sb, item, components, usedNames, counters);
            else if (type.Equals("Line", StringComparison.OrdinalIgnoreCase) || type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
                WriteShape(sb, item, type, components, usedNames, counters);
            else if (type.Equals("Image", StringComparison.OrdinalIgnoreCase))
                WriteImage(sb, item, components, usedNames, counters);
        }
    }

    private void WriteLabel(StringBuilder sb, LayoutItem item, List<ComponentRef> components,
        HashSet<string> usedNames, ComponentCounters counters)
    {
        counters.Label++;
        var name = BuildComponentName(usedNames, "QRLabel", item.Name, ref counters.Label);
        components.Add(new ComponentRef(name, "TQRLabel"));

        sb.AppendLine($"      object {name}: TQRLabel");
        sb.AppendLine($"        Left = {item.Left}");
        sb.AppendLine($"        Top = {item.Top}");
        sb.AppendLine($"        Width = {item.Width}");
        sb.AppendLine($"        Height = {item.Height}");
        sb.AppendLine($"        Alignment = {MapAlignment(item.Align)}");
        sb.AppendLine($"        Caption = {EncodeDelphi(item.Caption ?? string.Empty)}");
        sb.AppendLine("        AutoSize = False");
        sb.AppendLine($"        Transparent = {ToDelphiBool(item.Transparent ?? false)}");
        sb.AppendLine("        WordWrap = True");
        sb.AppendLine("        Font.Charset = DEFAULT_CHARSET");
        sb.AppendLine($"        Font.Color = {ToDelphiColor(item.TextColor, "clBlack")}");
        sb.AppendLine("        Font.Name = 'Gulim'");
        sb.AppendLine($"        Font.Size = {Math.Clamp(item.FontSize.GetValueOrDefault(10), MinFontSize, MaxFontSize)}");
        sb.AppendLine($"        Font.Style = {(item.Bold ?? false ? "[fsBold]" : "[]")}");
        sb.AppendLine("        ParentFont = False");
        sb.AppendLine($"        {ItemSizeValues(item).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
        sb.AppendLine("      end");
    }

    private void WriteShape(StringBuilder sb, LayoutItem item, string type, List<ComponentRef> components,
        HashSet<string> usedNames, ComponentCounters counters)
    {
        counters.Shape++;
        var name = BuildComponentName(usedNames, "QRShape", item.Name, ref counters.Shape);
        components.Add(new ComponentRef(name, "TQRShape"));

        sb.AppendLine($"      object {name}: TQRShape");
        sb.AppendLine($"        Left = {item.Left}");
        sb.AppendLine($"        Top = {item.Top}");
        sb.AppendLine($"        Width = {item.Width}");
        sb.AppendLine($"        Height = {item.Height}");
        sb.AppendLine($"        Pen.Width = {Math.Clamp(item.Thickness.GetValueOrDefault(1), 1, MaxPenThickness)}");
        sb.AppendLine($"        Pen.Color = {ToDelphiColor(item.StrokeColor, "clBlack")}");
        sb.AppendLine($"        Shape = {MapShape(type, item.Orientation)}");

        if (type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
        {
            var isFilled = (item.Filled ?? false) || !string.IsNullOrWhiteSpace(item.FillColor);
            if (isFilled)
            {
                sb.AppendLine("        Brush.Style = bsSolid");
                sb.AppendLine($"        Brush.Color = {ToDelphiColor(item.FillColor, "clWhite")}");
            }
            else
            {
                sb.AppendLine("        Brush.Style = bsClear");
            }
        }

        sb.AppendLine($"        {ItemSizeValues(item).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
        sb.AppendLine("      end");
    }

    private void WriteImage(StringBuilder sb, LayoutItem item, List<ComponentRef> components,
        HashSet<string> usedNames, ComponentCounters counters)
    {
        counters.Image++;
        var name = BuildComponentName(usedNames, "QRImage", item.Name, ref counters.Image);
        components.Add(new ComponentRef(name, "TQRImage"));

        sb.AppendLine($"      object {name}: TQRImage");
        sb.AppendLine($"        Left = {item.Left}");
        sb.AppendLine($"        Top = {item.Top}");
        sb.AppendLine($"        Width = {item.Width}");
        sb.AppendLine($"        Height = {item.Height}");
        sb.AppendLine($"        Stretch = {ToDelphiBool(item.Stretch ?? true)}");
        sb.AppendLine($"        {ItemSizeValues(item).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
        sb.AppendLine("      end");
    }

    private static void WriteClosingTags(StringBuilder sb)
    {
        sb.AppendLine("    end");  // DetailBand1
        sb.AppendLine("  end");    // QuickRep1
        sb.Append("end");          // Form
    }

    // ═══════════════════════════════════════════
    //  PAS 생성
    // ═══════════════════════════════════════════

    public string GeneratePas(string formName, string className, IReadOnlyCollection<ComponentRef> components, PasSpec? pasSpec)
    {
        var baseUnits = new[] { "Windows", "Messages", "SysUtils", "Variants", "Classes", "Graphics", "Controls", "Forms", "Dialogs", "QuickRpt", "QRCtrls", "ExtCtrls" };
        var extraUnits = NormalizeUnitNames(pasSpec?.Uses);
        var methods = NormalizeMethods(pasSpec?.Methods);

        var sb = new StringBuilder();

        // unit 선언
        sb.AppendLine($"unit {formName};");
        sb.AppendLine();

        // interface 섹션
        sb.AppendLine("interface");
        sb.AppendLine();
        AppendUsesClause(sb, baseUnits, extraUnits);
        sb.AppendLine();
        sb.AppendLine("type");
        sb.AppendLine($"  T{className} = class(TForm)");
        foreach (var c in components)
            sb.AppendLine($"    {c.Name}: {c.Type};");
        sb.AppendLine("  private");
        sb.AppendLine("  public");
        foreach (var m in methods)
            sb.AppendLine($"    {m.Declaration}");
        sb.AppendLine("  end;");
        sb.AppendLine();

        // var 섹션
        sb.AppendLine("var");
        sb.AppendLine($"  {formName}: T{className};");
        sb.AppendLine();

        // implementation 섹션
        sb.AppendLine("implementation");
        sb.AppendLine();
        sb.AppendLine("{$R *.dfm}");
        sb.AppendLine();
        foreach (var m in methods)
        {
            sb.AppendLine(BuildImplementationDeclaration(m.Declaration, className));
            sb.AppendLine("begin");
            foreach (var line in m.Body)
                sb.AppendLine(string.IsNullOrWhiteSpace(line) ? string.Empty : $"  {line.Trim()}");
            sb.AppendLine("end;");
            sb.AppendLine();
        }
        sb.Append("end.");

        return sb.ToString();
    }

    // ═══════════════════════════════════════════
    //  아이템 전처리 파이프라인
    // ═══════════════════════════════════════════

    private IReadOnlyList<LayoutItem> PrepareItems(IReadOnlyCollection<LayoutItem> sourceItems)
    {
        var items = NormalizeItems(sourceItems);
        items = DeduplicateItems(items);
        items = ClampItemsToWidth(items, MaxBandWidth);
        return items;
    }

    /// <summary>타입 매핑, 좌표 보정, 정렬.</summary>
    private static IReadOnlyList<LayoutItem> NormalizeItems(IReadOnlyCollection<LayoutItem> sourceItems)
    {
        var result = new List<LayoutItem>();
        foreach (var item in sourceItems)
        {
            var normalizedType = NormalizeItemType(item.Type);
            if (string.IsNullOrEmpty(normalizedType)) continue;

            var clone = item.Clone();
            clone.Type = normalizedType;
            clone.Caption = clone.Caption?.Trim();
            clone.Left = Math.Max(0, clone.Left);
            clone.Top = Math.Max(0, clone.Top);
            clone.Width = Math.Max(1, clone.Width);
            clone.Height = Math.Max(1, clone.Height);
            clone.FontSize = Math.Clamp(clone.FontSize.GetValueOrDefault(10), MinFontSize, MaxFontSize);
            clone.Thickness = Math.Clamp(clone.Thickness.GetValueOrDefault(1), 1, MaxPenThickness);

            if (clone.Type.Equals("Line", StringComparison.OrdinalIgnoreCase))
                clone.Orientation = InferLineOrientation(clone, item.Type);

            result.Add(clone);
        }

        return result.OrderBy(i => i.Top).ThenBy(i => i.Left).ToList();
    }

    /// <summary>동일 속성의 아이템 중복 제거.</summary>
    private static IReadOnlyList<LayoutItem> DeduplicateItems(IReadOnlyList<LayoutItem> items)
    {
        if (items.Count <= 1) return items;

        var seen = new HashSet<string>(StringComparer.Ordinal);
        var result = new List<LayoutItem>(items.Count);
        foreach (var item in items)
        {
            var key = string.Join("|",
                item.Type ?? "", item.Left, item.Top, item.Width, item.Height,
                item.Orientation ?? "", item.Caption ?? "", item.Align ?? "",
                item.FontSize, item.Bold, item.Transparent, item.TextColor ?? "",
                item.Thickness, item.StrokeColor ?? "", item.FillColor ?? "",
                item.Filled, item.Stretch);
            if (seen.Add(key))
                result.Add(item);
        }
        return result;
    }

    /// <summary>모든 아이템이 maxWidth 안에 들어오도록 클램핑.</summary>
    private static IReadOnlyList<LayoutItem> ClampItemsToWidth(IReadOnlyList<LayoutItem> items, int maxWidth)
    {
        if (items.Count == 0) return items;
        var currentMaxRight = items.Max(i => Math.Max(0, i.Left) + Math.Max(1, i.Width));
        if (currentMaxRight <= maxWidth) return items;

        return items.Select(item =>
        {
            var c = item.Clone();
            c.Left = Math.Clamp(c.Left, 0, maxWidth - 1);
            c.Width = Math.Max(1, Math.Min(c.Width, maxWidth - c.Left));
            return c;
        }).ToList();
    }

    /// <summary>밴드 경계 안으로 클램핑.</summary>
    private static IReadOnlyList<LayoutItem> ClampItemsToBand(IReadOnlyList<LayoutItem> items, int bandWidth, int bandHeight)
    {
        if (items.Count == 0) return items;

        return items.Select(item =>
        {
            var c = item.Clone();
            c.Left = Math.Clamp(c.Left, 0, Math.Max(1, bandWidth) - 1);
            c.Top = Math.Clamp(c.Top, 0, Math.Max(1, bandHeight) - 1);
            c.Width = Math.Max(1, Math.Min(c.Width, bandWidth - c.Left));
            c.Height = Math.Max(1, Math.Min(c.Height, bandHeight - c.Top));
            return c;
        }).ToList();
    }

    private static (int Width, int Height) CalculateBandSize(IReadOnlyCollection<LayoutItem> items)
    {
        if (items.Count == 0) return (MaxBandWidth, 1043);

        var maxRight = items.Max(i => Math.Max(0, i.Left) + Math.Max(1, i.Width));
        var maxBottom = items.Max(i => Math.Max(0, i.Top) + Math.Max(1, i.Height));

        return (
            Math.Clamp(maxRight + BandPadding, 64, MaxBandWidth),
            Math.Clamp(maxBottom + BandPadding, 32, MaxBandHeight));
    }

    // ═══════════════════════════════════════════
    //  DFM 인코딩 헬퍼
    // ═══════════════════════════════════════════

    /// <summary>
    /// 문자열을 Delphi DFM 형식으로 인코딩.
    /// ASCII는 '...'로 감싸고, 비ASCII 문자는 #유니코드값 으로 이스케이프한다.
    /// 예: "안녕" → #50504#45397
    /// </summary>
    public string EncodeDelphi(string text)
    {
        if (string.IsNullOrEmpty(text)) return "''";

        var result = new StringBuilder();
        var asciiBuffer = new StringBuilder();

        foreach (var ch in text)
        {
            if (ch is >= ' ' and <= '~')
            {
                asciiBuffer.Append(ch == '\'' ? "''" : ch);
                continue;
            }

            if (asciiBuffer.Length > 0)
            {
                result.Append('\'').Append(asciiBuffer).Append('\'');
                asciiBuffer.Clear();
            }
            result.Append('#').Append((int)ch);
        }

        if (asciiBuffer.Length > 0)
            result.Append('\'').Append(asciiBuffer).Append('\'');

        return result.Length == 0 ? "''" : result.ToString();
    }

    /// <summary>DFM의 Size.Values 속성 (픽셀 → mm 변환).</summary>
    private string ItemSizeValues(LayoutItem item) =>
        FormatSizeValues(item.Height, item.Left, item.Top, item.Width);

    private string FormatSizeValues(int height, int left, int top, int width) =>
        $"Size.Values = ({Environment.NewLine}" +
        $"  {(height * PixelToMm).ToString("F12", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
        $"  {(left * PixelToMm).ToString("F12", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
        $"  {(top * PixelToMm).ToString("F12", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
        $"  {(width * PixelToMm).ToString("F12", CultureInfo.InvariantCulture)})";

    private static string BandSizeValues(int height, int width)
    {
        var hMm = Math.Round(height * PixelToMm, 15, MidpointRounding.AwayFromZero);
        var wMm = Math.Round(width * PixelToMm, 15, MidpointRounding.AwayFromZero);
        return $"Size.Values = ({Environment.NewLine}" +
               $"  {hMm.ToString("F18", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
               $"  {wMm.ToString("F18", CultureInfo.InvariantCulture)})";
    }

    // ═══════════════════════════════════════════
    //  Delphi 값 변환
    // ═══════════════════════════════════════════

    /// <summary>
    /// 다양한 색상 형식(#RRGGBB, $00BBGGRR, clXxx)을 Delphi 색상 문자열로 변환.
    /// HTML #RRGGBB → Delphi $00BBGGRR (바이트 순서 반전).
    /// </summary>
    private static string ToDelphiColor(string? rawColor, string fallback)
    {
        if (string.IsNullOrWhiteSpace(rawColor)) return fallback;

        var value = rawColor.Trim();
        if (value.StartsWith("cl", StringComparison.OrdinalIgnoreCase)) return value;
        if (value.StartsWith("$", StringComparison.Ordinal) && value.Length == 9) return value;

        if (value.StartsWith("#", StringComparison.Ordinal))
        {
            var hex = value[1..];
            if (hex.Length == 8) hex = hex[2..]; // #AARRGGBB → RRGGBB

            if (hex.Length == 6 &&
                int.TryParse(hex[..2], NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var r) &&
                int.TryParse(hex.Substring(2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var g) &&
                int.TryParse(hex.Substring(4, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var b))
                return $"$00{b:X2}{g:X2}{r:X2}";
        }

        return fallback;
    }

    private static string ToDelphiBool(bool value) => value ? "True" : "False";

    private static string MapAlignment(string? align) =>
        (align ?? "Left").Trim().ToLowerInvariant() switch
        {
            "center" => "taCenter",
            "right" => "taRightJustify",
            _ => "taLeftJustify"
        };

    private static string MapShape(string type, string? orientation)
    {
        if (type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
            return "qrsRectangle";

        return (orientation ?? "H").Trim().ToUpperInvariant() switch
        {
            "V" => "qrsVertLine",
            _ => "qrsHorLine"
        };
    }

    // ═══════════════════════════════════════════
    //  타입 · 방향 정규화
    // ═══════════════════════════════════════════

    /// <summary>다양한 타입 문자열("label", "box" 등)을 표준 타입으로 매핑.</summary>
    private static string NormalizeItemType(string? rawType)
    {
        var compact = CompactString(rawType);
        return compact switch
        {
            "text" or "label" or "caption" or "memo" or "string" => "Text",
            "line" or "hline" or "vline" or "horline" or "vertline" or "horizontalline" or "verticalline" => "Line",
            "rect" or "rectangle" or "box" or "cell" or "tablecell" => "Rect",
            "image" or "img" or "picture" or "photo" or "logo" => "Image",
            _ => string.Empty
        };
    }

    /// <summary>선의 방향을 타입명/속성/크기에서 추론.</summary>
    private static string InferLineOrientation(LayoutItem item, string? originalType)
    {
        var compact = CompactString(originalType);
        if (compact.Contains("vline", StringComparison.Ordinal) || compact.Contains("vert", StringComparison.Ordinal))
            return "V";
        if (compact.Contains("hline", StringComparison.Ordinal) || compact.Contains("hor", StringComparison.Ordinal))
            return "H";
        if ((item.Orientation ?? "").Trim().Equals("V", StringComparison.OrdinalIgnoreCase))
            return "V";
        return item.Height > item.Width ? "V" : "H";
    }

    /// <summary>공백/특수문자를 제거하고 소문자로 변환.</summary>
    private static string CompactString(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return string.Empty;
        return new string(value.Trim().ToLowerInvariant().Where(char.IsLetterOrDigit).ToArray());
    }

    // ═══════════════════════════════════════════
    //  PAS 헬퍼
    // ═══════════════════════════════════════════

    private static void AppendUsesClause(StringBuilder sb, IEnumerable<string> baseUnits, IEnumerable<string> extraUnits)
    {
        var allUnits = baseUnits
            .Concat(extraUnits)
            .Where(u => !string.IsNullOrWhiteSpace(u))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();

        sb.AppendLine("uses");
        for (var i = 0; i < allUnits.Count; i++)
            sb.AppendLine($"  {allUnits[i]}{(i == allUnits.Count - 1 ? ";" : ",")}");
    }

    private static IReadOnlyList<string> NormalizeUnitNames(IReadOnlyCollection<string>? units)
    {
        if (units is null or { Count: 0 }) return [];
        return units
            .Select(u => (u ?? "").Trim().TrimEnd(';').Trim())
            .Where(u => !string.IsNullOrWhiteSpace(u))
            .ToList();
    }

    private static IReadOnlyList<(string Declaration, IReadOnlyList<string> Body)> NormalizeMethods(IReadOnlyCollection<PasMethodSpec>? methods)
    {
        if (methods is null or { Count: 0 }) return [];

        return methods
            .Select(m =>
            {
                var decl = EnsureSemicolon(m.Declaration);
                if (string.IsNullOrWhiteSpace(decl)) return default;

                var body = (m.Body ?? [])
                    .Select(l => (l ?? "").Trim())
                    .Where(l =>
                        !l.Equals("begin", StringComparison.OrdinalIgnoreCase) &&
                        !l.Equals("end;", StringComparison.OrdinalIgnoreCase) &&
                        !l.Equals("end", StringComparison.OrdinalIgnoreCase))
                    .ToList();

                if (body.Count == 0) body.Add("// TODO");
                return (Declaration: decl, Body: (IReadOnlyList<string>)body);
            })
            .Where(m => !string.IsNullOrWhiteSpace(m.Declaration))
            .ToList();
    }

    private static string EnsureSemicolon(string declaration)
    {
        var clean = (declaration ?? "").Trim();
        return string.IsNullOrWhiteSpace(clean) ? string.Empty
            : clean.EndsWith(";", StringComparison.Ordinal) ? clean : clean + ";";
    }

    /// <summary>
    /// 인터페이스 선언("procedure Foo;")을 구현부 선언("procedure TClassName.Foo;")으로 변환.
    /// </summary>
    private static string BuildImplementationDeclaration(string declaration, string className)
    {
        var clean = EnsureSemicolon(declaration);
        if (string.IsNullOrWhiteSpace(clean)) return string.Empty;

        var prefix = $"T{className}.";
        if (clean.Contains(prefix, StringComparison.Ordinal)) return clean;

        var lowered = clean.ToLowerInvariant();
        string[] keywords = ["class procedure ", "class function ", "procedure ", "function ", "constructor ", "destructor "];
        foreach (var kw in keywords)
        {
            if (lowered.StartsWith(kw, StringComparison.Ordinal))
                return $"{kw.TrimStart()}{prefix}{clean[kw.Length..]}";
        }

        return $"procedure {prefix}{clean}";
    }

    // ═══════════════════════════════════════════
    //  컴포넌트 이름 생성
    // ═══════════════════════════════════════════

    private static string BuildComponentName(HashSet<string> usedNames, string fallbackPrefix, string? preferred, ref int counter)
    {
        var sanitized = SanitizeIdentifier(preferred);
        var candidate = string.IsNullOrWhiteSpace(sanitized)
            ? $"{fallbackPrefix}{counter}"
            : sanitized!;

        if (!char.IsLetter(candidate[0]) && candidate[0] != '_')
            candidate = $"{fallbackPrefix}_{candidate}";

        if (usedNames.Add(candidate)) return candidate;

        var suffix = 2;
        while (!usedNames.Add($"{candidate}_{suffix}")) suffix++;
        return $"{candidate}_{suffix}";
    }

    private static string? SanitizeIdentifier(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;

        var chars = value.Trim()
            .Select(ch => (ch is >= 'A' and <= 'Z') || (ch is >= 'a' and <= 'z') || (ch is >= '0' and <= '9') || ch == '_' ? ch : '_')
            .ToArray();
        var candidate = new string(chars);
        while (candidate.Contains("__", StringComparison.Ordinal))
            candidate = candidate.Replace("__", "_", StringComparison.Ordinal);
        candidate = candidate.Trim('_');

        if (string.IsNullOrWhiteSpace(candidate)) return null;
        return candidate.Length > 48 ? candidate[..48] : candidate;
    }

    // ═══════════════════════════════════════════
    //  유틸리티
    // ═══════════════════════════════════════════

    private static void WriteEntry(ZipArchive archive, string entryName, string content)
    {
        var entry = archive.CreateEntry(entryName);
        using var writer = new StreamWriter(entry.Open(), Encoding.ASCII);
        writer.Write(content);
    }

    private sealed class ComponentCounters
    {
        public int Label;
        public int Shape;
        public int Image;
    }
}

/// <summary>DFM에서 컴포넌트 하나의 이름과 Delphi 타입.</summary>
public sealed record ComponentRef(string Name, string Type);
