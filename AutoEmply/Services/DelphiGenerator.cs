using System.Globalization;
using System.IO.Compression;
using System.Text;
using AutoEmply.Models;

namespace AutoEmply.Services;

public sealed class DelphiGenerator
{
    private const double Mm = 2.645833333333333;
    private const int QuickRepWidth = 794;
    private const int DetailBandLeft = 0;
    private const int MaxDetailBandWidth = QuickRepWidth;
    private const int MaxDetailBandHeight = 6000;
    private const int DetailBandPadding = 2;
    private const int MinFontSize = 6;
    private const int MaxFontSize = 24;
    private const int MaxPenThickness = 6;

    public byte[] GenerateZip(string formName, LayoutSpec layoutSpec)
    {
        var className = formName.Replace("_", string.Empty, StringComparison.Ordinal);
        var dfm = GenerateDfm(formName, className, layoutSpec, out var components);
        var pas = GeneratePas(formName, className, components, layoutSpec.Pas);

        using var stream = new MemoryStream();
        using (var archive = new ZipArchive(stream, ZipArchiveMode.Create, true))
        {
            var dfmEntry = archive.CreateEntry($"{formName}.dfm");
            using (var writer = new StreamWriter(dfmEntry.Open(), Encoding.ASCII))
            {
                writer.Write(dfm);
            }

            var pasEntry = archive.CreateEntry($"{formName}.pas");
            using (var writer = new StreamWriter(pasEntry.Open(), Encoding.ASCII))
            {
                writer.Write(pas);
            }
        }

        return stream.ToArray();
    }

    public string Enc(string text)
    {
        if (string.IsNullOrEmpty(text))
        {
            return "''";
        }

        var result = new StringBuilder();
        var asciiBuffer = new StringBuilder();

        static bool IsAscii(char ch) => ch is >= ' ' and <= '~';

        foreach (var ch in text)
        {
            if (IsAscii(ch))
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
        {
            result.Append('\'').Append(asciiBuffer).Append('\'');
        }

        return result.Length == 0 ? "''" : result.ToString();
    }

    public string SizeValues(int height, int left, int top, int width)
    {
        return $"Size.Values = ({Environment.NewLine}" +
               $"  {(height * Mm).ToString("F12", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
               $"  {(left * Mm).ToString("F12", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
               $"  {(top * Mm).ToString("F12", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
               $"  {(width * Mm).ToString("F12", CultureInfo.InvariantCulture)})";
    }

    public string DetailBandSizeValues(int height, int width)
    {
        var heightMm = Math.Round(height * Mm, 15, MidpointRounding.AwayFromZero);
        var widthMm = Math.Round(width * Mm, 15, MidpointRounding.AwayFromZero);

        return $"Size.Values = ({Environment.NewLine}" +
               $"  {heightMm.ToString("F18", CultureInfo.InvariantCulture)}{Environment.NewLine}" +
               $"  {widthMm.ToString("F18", CultureInfo.InvariantCulture)})";
    }

    public string GenerateDfm(string formName, string className, LayoutSpec layoutSpec, out List<ComponentRef> components)
    {
        var sb = new StringBuilder();
        components = [new ComponentRef("QuickRep1", "TQuickRep"), new ComponentRef("DetailBand1", "TQRBand")];
        var normalizedItems = NormalizeLayoutItems(layoutSpec.Items);
        normalizedItems = DeduplicateLayoutItems(normalizedItems);
        normalizedItems = FitItemsToBandWidth(normalizedItems, MaxDetailBandWidth);
        var detailBandSize = CalculateDetailBandSize(normalizedItems);
        normalizedItems = FitItemsToDetailBandBounds(normalizedItems, detailBandSize.Width, detailBandSize.Height);

        sb.AppendLine($"object {formName}: T{className}");
        sb.AppendLine("  Left = 0");
        sb.AppendLine("  Top = 0");
        sb.AppendLine("  Width = 800");
        sb.AppendLine("  Height = 600");
        sb.AppendLine($"  Caption = {Enc(formName)}");
        sb.AppendLine("  OldCreateOrder = False");
        sb.AppendLine("  PixelsPerInch = 96");
        sb.AppendLine("  TextHeight = 13");
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
        sb.AppendLine("    object DetailBand1: TQRBand");
        sb.AppendLine($"      Left = {DetailBandLeft}");
        sb.AppendLine("      Top = 38");
        sb.AppendLine($"      Width = {detailBandSize.Width}");
        sb.AppendLine($"      Height = {detailBandSize.Height}");
        sb.AppendLine("      BandType = rbDetail");
        sb.AppendLine($"      {DetailBandSizeValues(detailBandSize.Height, detailBandSize.Width).Replace(Environment.NewLine, Environment.NewLine + "      ", StringComparison.Ordinal)}");

        var labelNo = 0;
        var shapeNo = 0;
        var imageNo = 0;
        var usedComponentNames = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var item in normalizedItems)
        {
            var type = item.Type?.Trim() ?? string.Empty;
            if (type.Equals("Text", StringComparison.OrdinalIgnoreCase))
            {
                labelNo++;
                var name = BuildComponentName(usedComponentNames, "QRLabel", item.Name, ref labelNo);
                components.Add(new ComponentRef(name, "TQRLabel"));
                sb.AppendLine($"      object {name}: TQRLabel");
                sb.AppendLine($"        Left = {item.Left}");
                sb.AppendLine($"        Top = {item.Top}");
                sb.AppendLine($"        Width = {item.Width}");
                sb.AppendLine($"        Height = {item.Height}");
                sb.AppendLine($"        Alignment = {MapAlign(item.Align)}");
                sb.AppendLine($"        Caption = {Enc(item.Caption ?? string.Empty)}");
                sb.AppendLine("        AutoSize = False");
                sb.AppendLine($"        Transparent = {ToDelphiBool(item.Transparent ?? false)}");
                sb.AppendLine("        WordWrap = True");
                sb.AppendLine("        Font.Charset = DEFAULT_CHARSET");
                sb.AppendLine($"        Font.Color = {NormalizeDelphiColor(item.TextColor, "clBlack")}");
                sb.AppendLine("        Font.Name = 'Gulim'");
                sb.AppendLine($"        Font.Size = {Math.Clamp(item.FontSize.GetValueOrDefault(10), MinFontSize, MaxFontSize)}");
                sb.AppendLine($"        Font.Style = {(item.Bold ?? false ? "[fsBold]" : "[]")}");
                sb.AppendLine("        ParentFont = False");
                sb.AppendLine($"        {SizeValues(item.Height, item.Left, item.Top, item.Width).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
                sb.AppendLine("      end");
                continue;
            }

            if (type.Equals("Line", StringComparison.OrdinalIgnoreCase) || type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
            {
                shapeNo++;
                var name = BuildComponentName(usedComponentNames, "QRShape", item.Name, ref shapeNo);
                components.Add(new ComponentRef(name, "TQRShape"));
                sb.AppendLine($"      object {name}: TQRShape");
                sb.AppendLine($"        Left = {item.Left}");
                sb.AppendLine($"        Top = {item.Top}");
                sb.AppendLine($"        Width = {item.Width}");
                sb.AppendLine($"        Height = {item.Height}");
                sb.AppendLine($"        Pen.Width = {Math.Clamp(item.Thickness.GetValueOrDefault(1), 1, MaxPenThickness)}");
                sb.AppendLine($"        Pen.Color = {NormalizeDelphiColor(item.StrokeColor, "clBlack")}");
                sb.AppendLine($"        Shape = {MapShape(type, item.Orientation)}");
                if (type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
                {
                    var isFilledRect = (item.Filled ?? false) || !string.IsNullOrWhiteSpace(item.FillColor);
                    if (isFilledRect)
                    {
                        sb.AppendLine("        Brush.Style = bsSolid");
                        sb.AppendLine($"        Brush.Color = {NormalizeDelphiColor(item.FillColor, "clWhite")}");
                    }
                    else
                    {
                        sb.AppendLine("        Brush.Style = bsClear");
                    }
                }
                sb.AppendLine($"        {SizeValues(item.Height, item.Left, item.Top, item.Width).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
                sb.AppendLine("      end");
                continue;
            }

            if (type.Equals("Image", StringComparison.OrdinalIgnoreCase))
            {
                imageNo++;
                var name = BuildComponentName(usedComponentNames, "QRImage", item.Name, ref imageNo);
                components.Add(new ComponentRef(name, "TQRImage"));
                sb.AppendLine($"      object {name}: TQRImage");
                sb.AppendLine($"        Left = {item.Left}");
                sb.AppendLine($"        Top = {item.Top}");
                sb.AppendLine($"        Width = {item.Width}");
                sb.AppendLine($"        Height = {item.Height}");
                sb.AppendLine($"        Stretch = {ToDelphiBool(item.Stretch ?? true)}");
                sb.AppendLine($"        {SizeValues(item.Height, item.Left, item.Top, item.Width).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
                sb.AppendLine("      end");
            }
        }

        sb.AppendLine("    end");
        sb.AppendLine("  end");
        sb.Append("end");
        return sb.ToString();
    }

    private static (int Width, int Height) CalculateDetailBandSize(IReadOnlyCollection<LayoutItem> items)
    {
        if (items.Count == 0)
        {
            return (MaxDetailBandWidth, 1043);
        }

        const int minWidth = 64;
        const int minHeight = 32;

        var maxRight = items.Max(item => Math.Max(0, item.Left) + Math.Max(1, item.Width));
        var maxBottom = items.Max(item => Math.Max(0, item.Top) + Math.Max(1, item.Height));

        var width = Math.Max(minWidth, Math.Min(MaxDetailBandWidth, maxRight + DetailBandPadding));
        var height = Math.Max(minHeight, Math.Min(MaxDetailBandHeight, maxBottom + DetailBandPadding));
        return (width, height);
    }

    private static IReadOnlyList<LayoutItem> FitItemsToBandWidth(IReadOnlyList<LayoutItem> items, int maxContentRight)
    {
        if (items.Count == 0)
        {
            return items;
        }

        var currentMaxRight = items.Max(item => Math.Max(0, item.Left) + Math.Max(1, item.Width));
        if (currentMaxRight <= maxContentRight)
        {
            return items;
        }

        var clamped = new List<LayoutItem>(items.Count);
        foreach (var item in items)
        {
            var clone = CloneItem(item);
            clone.Left = Math.Max(0, clone.Left);
            clone.Top = Math.Max(0, clone.Top);
            clone.Width = Math.Max(1, clone.Width);
            clone.Height = Math.Max(1, clone.Height);

            if (clone.Left > maxContentRight - 1)
            {
                clone.Left = Math.Max(0, maxContentRight - 1);
                clone.Width = 1;
            }

            var right = clone.Left + clone.Width;
            if (right > maxContentRight)
            {
                clone.Width = Math.Max(1, maxContentRight - clone.Left);
            }

            clamped.Add(clone);
        }

        return clamped;
    }

    private static IReadOnlyList<LayoutItem> FitItemsToDetailBandBounds(IReadOnlyList<LayoutItem> items, int bandWidth, int bandHeight)
    {
        if (items.Count == 0)
        {
            return items;
        }

        var safeRight = Math.Max(1, bandWidth);
        var safeBottom = Math.Max(1, bandHeight);
        var clamped = new List<LayoutItem>(items.Count);
        foreach (var item in items)
        {
            var clone = CloneItem(item);
            clone.Left = Math.Clamp(clone.Left, 0, safeRight - 1);
            clone.Top = Math.Clamp(clone.Top, 0, safeBottom - 1);
            clone.Width = Math.Max(1, clone.Width);
            clone.Height = Math.Max(1, clone.Height);

            var right = clone.Left + clone.Width;
            var bottom = clone.Top + clone.Height;
            if (right > safeRight)
            {
                clone.Width = Math.Max(1, safeRight - clone.Left);
            }

            if (bottom > safeBottom)
            {
                clone.Height = Math.Max(1, safeBottom - clone.Top);
            }

            clamped.Add(clone);
        }

        return clamped;
    }

    public string GeneratePas(string formName, string className, IReadOnlyCollection<ComponentRef> components, PasSpec? pasSpec)
    {
        var baseUnits = new[]
        {
            "Windows",
            "Messages",
            "SysUtils",
            "Variants",
            "Classes",
            "Graphics",
            "Controls",
            "Forms",
            "Dialogs",
            "QuickRpt",
            "QRCtrls",
            "ExtCtrls"
        };
        var extraUnits = NormalizeUnitNames(pasSpec?.Uses);
        var methodSpecs = NormalizeMethods(pasSpec?.Methods);

        var sb = new StringBuilder();
        sb.AppendLine($"unit {formName};");
        sb.AppendLine();
        sb.AppendLine("interface");
        sb.AppendLine();
        AppendUsesClause(sb, baseUnits, extraUnits);
        sb.AppendLine();
        sb.AppendLine("type");
        sb.AppendLine($"  T{className} = class(TForm)");
        foreach (var component in components)
        {
            sb.AppendLine($"    {component.Name}: {component.Type};");
        }

        sb.AppendLine("  private");
        sb.AppendLine("  public");
        foreach (var method in methodSpecs)
        {
            sb.AppendLine($"    {method.Declaration}");
        }

        sb.AppendLine("  end;");
        sb.AppendLine();
        sb.AppendLine("var");
        sb.AppendLine($"  {formName}: T{className};");
        sb.AppendLine();
        sb.AppendLine("implementation");
        sb.AppendLine();
        sb.AppendLine("{$R *.dfm}");
        sb.AppendLine();
        foreach (var method in methodSpecs)
        {
            sb.AppendLine(BuildImplementationDeclaration(method.Declaration, className));
            sb.AppendLine("begin");
            foreach (var line in method.Body)
            {
                if (string.IsNullOrWhiteSpace(line))
                {
                    sb.AppendLine();
                    continue;
                }

                sb.AppendLine($"  {line.Trim()}");
            }

            sb.AppendLine("end;");
            sb.AppendLine();
        }

        sb.Append("end.");
        return sb.ToString();
    }

    private static void AppendUsesClause(StringBuilder sb, IEnumerable<string> baseUnits, IEnumerable<string> extraUnits)
    {
        var allUnits = baseUnits
            .Concat(extraUnits)
            .Where(static x => !string.IsNullOrWhiteSpace(x))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();

        sb.AppendLine("uses");
        for (var i = 0; i < allUnits.Count; i++)
        {
            var suffix = i == allUnits.Count - 1 ? ";" : ",";
            sb.AppendLine($"  {allUnits[i]}{suffix}");
        }
    }

    private static IReadOnlyList<string> NormalizeUnitNames(IReadOnlyCollection<string>? units)
    {
        if (units is null || units.Count == 0)
        {
            return [];
        }

        var result = new List<string>();
        foreach (var rawUnit in units)
        {
            var unitName = (rawUnit ?? string.Empty).Trim();
            if (string.IsNullOrWhiteSpace(unitName))
            {
                continue;
            }

            if (unitName.EndsWith(";", StringComparison.Ordinal))
            {
                unitName = unitName[..^1].Trim();
            }

            if (string.IsNullOrWhiteSpace(unitName))
            {
                continue;
            }

            result.Add(unitName);
        }

        return result;
    }

    private static IReadOnlyList<(string Declaration, IReadOnlyList<string> Body)> NormalizeMethods(IReadOnlyCollection<PasMethodSpec>? methods)
    {
        if (methods is null || methods.Count == 0)
        {
            return [];
        }

        var result = new List<(string Declaration, IReadOnlyList<string> Body)>();
        foreach (var method in methods)
        {
            var declaration = EnsureDeclarationSuffix(method.Declaration);
            if (string.IsNullOrWhiteSpace(declaration))
            {
                continue;
            }

            var bodyLines = (method.Body ?? [])
                .Select(static x => x ?? string.Empty)
                .Select(static x => x.Trim())
                .Where(static x =>
                    !string.Equals(x, "begin", StringComparison.OrdinalIgnoreCase) &&
                    !string.Equals(x, "end;", StringComparison.OrdinalIgnoreCase) &&
                    !string.Equals(x, "end", StringComparison.OrdinalIgnoreCase))
                .ToList();

            if (bodyLines.Count == 0)
            {
                bodyLines.Add("// TODO");
            }

            result.Add((declaration, bodyLines));
        }

        return result;
    }

    private static string EnsureDeclarationSuffix(string declaration)
    {
        var clean = (declaration ?? string.Empty).Trim();
        if (string.IsNullOrWhiteSpace(clean))
        {
            return string.Empty;
        }

        if (!clean.EndsWith(";", StringComparison.Ordinal))
        {
            clean += ";";
        }

        return clean;
    }

    private static string BuildImplementationDeclaration(string declaration, string className)
    {
        var clean = EnsureDeclarationSuffix(declaration);
        if (string.IsNullOrWhiteSpace(clean))
        {
            return string.Empty;
        }

        var lowered = clean.ToLowerInvariant();
        var classPrefix = $"T{className}.";
        if (clean.Contains(classPrefix, StringComparison.Ordinal))
        {
            return clean;
        }

        if (lowered.StartsWith("class procedure ", StringComparison.Ordinal))
        {
            return $"class procedure {classPrefix}{clean["class procedure ".Length..]}";
        }

        if (lowered.StartsWith("class function ", StringComparison.Ordinal))
        {
            return $"class function {classPrefix}{clean["class function ".Length..]}";
        }

        if (lowered.StartsWith("procedure ", StringComparison.Ordinal))
        {
            return $"procedure {classPrefix}{clean["procedure ".Length..]}";
        }

        if (lowered.StartsWith("function ", StringComparison.Ordinal))
        {
            return $"function {classPrefix}{clean["function ".Length..]}";
        }

        if (lowered.StartsWith("constructor ", StringComparison.Ordinal))
        {
            return $"constructor {classPrefix}{clean["constructor ".Length..]}";
        }

        if (lowered.StartsWith("destructor ", StringComparison.Ordinal))
        {
            return $"destructor {classPrefix}{clean["destructor ".Length..]}";
        }

        return $"procedure {classPrefix}{clean}";
    }

    private static IReadOnlyList<LayoutItem> NormalizeLayoutItems(IReadOnlyCollection<LayoutItem> sourceItems)
    {
        if (sourceItems.Count == 0)
        {
            return [];
        }

        var expanded = new List<LayoutItem>();
        foreach (var item in sourceItems)
        {
            var clone = CloneItem(item);
            var normalizedType = NormalizeItemType(clone.Type);
            if (string.IsNullOrEmpty(normalizedType))
            {
                continue;
            }

            clone.Type = normalizedType;
            clone.Caption = clone.Caption?.Trim();
            clone.Left = Math.Max(0, clone.Left);
            clone.Top = Math.Max(0, clone.Top);
            clone.Width = Math.Max(1, clone.Width);
            clone.Height = Math.Max(1, clone.Height);
            clone.FontSize = Math.Clamp(clone.FontSize.GetValueOrDefault(10), MinFontSize, MaxFontSize);
            clone.Thickness = Math.Clamp(clone.Thickness.GetValueOrDefault(1), 1, MaxPenThickness);

            if (clone.Type.Equals("Line", StringComparison.OrdinalIgnoreCase))
            {
                clone.Orientation = NormalizeLineOrientation(clone, item.Type);
            }

            expanded.Add(clone);
        }

        return expanded
            .OrderBy(item => item.Top)
            .ThenBy(item => item.Left)
            .ToList();
    }

    private static IReadOnlyList<LayoutItem> DeduplicateLayoutItems(IReadOnlyList<LayoutItem> items)
    {
        if (items.Count <= 1)
        {
            return items;
        }

        var deduped = new List<LayoutItem>(items.Count);
        var seen = new HashSet<string>(StringComparer.Ordinal);
        foreach (var item in items)
        {
            var key = string.Join("|",
                item.Type ?? string.Empty,
                item.Left,
                item.Top,
                item.Width,
                item.Height,
                item.Orientation ?? string.Empty,
                item.Caption ?? string.Empty,
                item.Align ?? string.Empty,
                item.FontSize,
                item.Bold,
                item.Transparent,
                item.TextColor ?? string.Empty,
                item.Thickness,
                item.StrokeColor ?? string.Empty,
                item.FillColor ?? string.Empty,
                item.Filled,
                item.Stretch);
            if (!seen.Add(key))
            {
                continue;
            }

            deduped.Add(item);
        }

        return deduped;
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

    private static string NormalizeDelphiColor(string? rawColor, string fallback)
    {
        if (string.IsNullOrWhiteSpace(rawColor))
        {
            return fallback;
        }

        var value = rawColor.Trim();
        if (value.StartsWith("cl", StringComparison.OrdinalIgnoreCase))
        {
            return value;
        }

        if (value.StartsWith("$", StringComparison.Ordinal) && value.Length == 9)
        {
            return value;
        }

        if (value.StartsWith("#", StringComparison.Ordinal))
        {
            var hex = value[1..];
            if (hex.Length == 8)
            {
                hex = hex[2..];
            }

            if (hex.Length == 6 &&
                int.TryParse(hex[..2], NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var r) &&
                int.TryParse(hex.Substring(2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var g) &&
                int.TryParse(hex.Substring(4, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var b))
            {
                return $"$00{b:X2}{g:X2}{r:X2}";
            }
        }

        return fallback;
    }

    private static string ToDelphiBool(bool value) => value ? "True" : "False";

    private static string MapAlign(string? align)
    {
        return (align ?? "Left").Trim().ToLowerInvariant() switch
        {
            "center" => "taCenter",
            "right" => "taRightJustify",
            _ => "taLeftJustify"
        };
    }

    private static string MapShape(string type, string? orientation)
    {
        if (type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
        {
            return "qrsRectangle";
        }

        return (orientation ?? "H").Trim().ToUpperInvariant() switch
        {
            "V" => "qrsVertLine",
            _ => "qrsHorLine"
        };
    }

    private static string NormalizeLineOrientation(LayoutItem item, string? originalType)
    {
        var compactType = Compact(originalType);
        if (compactType.Contains("vline", StringComparison.Ordinal) || compactType.Contains("vert", StringComparison.Ordinal))
        {
            return "V";
        }

        if (compactType.Contains("hline", StringComparison.Ordinal) || compactType.Contains("hor", StringComparison.Ordinal))
        {
            return "H";
        }

        if ((item.Orientation ?? string.Empty).Trim().Equals("V", StringComparison.OrdinalIgnoreCase))
        {
            return "V";
        }

        if (item.Height > item.Width)
        {
            return "V";
        }

        return "H";
    }

    private static string NormalizeItemType(string? rawType)
    {
        var compact = Compact(rawType);
        return compact switch
        {
            "text" or "label" or "caption" or "memo" or "string" => "Text",
            "line" or "hline" or "vline" or "horline" or "vertline" or "horizontalline" or "verticalline" => "Line",
            "rect" or "rectangle" or "box" or "cell" or "tablecell" => "Rect",
            "image" or "img" or "picture" or "photo" or "logo" => "Image",
            _ => string.Empty
        };
    }

    private static string Compact(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return string.Empty;
        }

        return new string(value
            .Trim()
            .ToLowerInvariant()
            .Where(char.IsLetterOrDigit)
            .ToArray());
    }

    private static bool ShouldWordWrap(LayoutItem item)
    {
        var caption = item.Caption ?? string.Empty;
        if (string.IsNullOrWhiteSpace(caption))
        {
            return false;
        }

        if (caption.Contains('\n', StringComparison.Ordinal) || caption.Contains('\r', StringComparison.Ordinal))
        {
            return true;
        }

        var fontSize = Math.Clamp(item.FontSize.GetValueOrDefault(10), MinFontSize, MaxFontSize);
        var lineHeight = Math.Max(1, (int)Math.Round(fontSize * 1.2, MidpointRounding.AwayFromZero));
        if (item.Height < lineHeight * 2)
        {
            return false;
        }

        var estimatedCharsPerLine = Math.Max(1, item.Width / Math.Max(1, (int)Math.Round(fontSize * 0.75, MidpointRounding.AwayFromZero)));
        return caption.Length > estimatedCharsPerLine;
    }

    private static string BuildComponentName(HashSet<string> usedNames, string fallbackPrefix, string? preferred, ref int counter)
    {
        var sanitized = SanitizeDelphiIdentifier(preferred);
        var candidate = string.IsNullOrWhiteSpace(sanitized)
            ? $"{fallbackPrefix}{counter}"
            : sanitized!;

        if (!char.IsLetter(candidate[0]) && candidate[0] != '_')
        {
            candidate = $"{fallbackPrefix}_{candidate}";
        }

        if (usedNames.Add(candidate))
        {
            return candidate;
        }

        var seed = candidate;
        var suffix = 2;
        while (!usedNames.Add($"{seed}_{suffix}"))
        {
            suffix++;
        }

        return $"{seed}_{suffix}";
    }

    private static string? SanitizeDelphiIdentifier(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return null;
        }

        static bool IsAsciiLetterOrDigit(char ch) =>
            (ch is >= 'A' and <= 'Z') || (ch is >= 'a' and <= 'z') || (ch is >= '0' and <= '9');

        var chars = value
            .Trim()
            .Select(ch => IsAsciiLetterOrDigit(ch) || ch == '_' ? ch : '_')
            .ToArray();
        var candidate = new string(chars);
        while (candidate.Contains("__", StringComparison.Ordinal))
        {
            candidate = candidate.Replace("__", "_", StringComparison.Ordinal);
        }

        candidate = candidate.Trim('_');
        if (string.IsNullOrWhiteSpace(candidate))
        {
            return null;
        }

        return candidate.Length > 48 ? candidate[..48] : candidate;
    }
}

public sealed record ComponentRef(string Name, string Type);
