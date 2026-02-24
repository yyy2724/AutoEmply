using System.Globalization;
using System.IO.Compression;
using System.Text;
using AutoEmply.Models;

namespace AutoEmply.Services;

public sealed class DelphiGenerator
{
    private const double Mm = 2.645833333333333;

    public byte[] GenerateZip(string formName, LayoutSpec layoutSpec)
    {
        var className = formName.Replace("_", string.Empty, StringComparison.Ordinal);
        var dfm = GenerateDfm(formName, className, layoutSpec, out var components);
        var pas = GeneratePas(formName, className, components);

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
        sb.AppendLine("    Width = 794");
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
        sb.AppendLine("      Left = 38");
        sb.AppendLine("      Top = 38");
        sb.AppendLine("      Width = 718");
        sb.AppendLine("      Height = 1043");
        sb.AppendLine("      BandType = rbDetail");
        sb.AppendLine($"      {DetailBandSizeValues(1043, 718).Replace(Environment.NewLine, Environment.NewLine + "      ", StringComparison.Ordinal)}");

        var labelNo = 0;
        var shapeNo = 0;
        var imageNo = 0;

        foreach (var item in layoutSpec.Items)
        {
            var type = item.Type?.Trim() ?? string.Empty;
            if (type.Equals("Text", StringComparison.OrdinalIgnoreCase))
            {
                labelNo++;
                var name = $"QRLabel{labelNo}";
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
                sb.AppendLine("        Font.Color = clBlack");
                sb.AppendLine("        Font.Name = 'Arial'");
                sb.AppendLine($"        Font.Size = {item.FontSize.GetValueOrDefault(10)}");
                sb.AppendLine($"        Font.Style = {(item.Bold ?? false ? "[fsBold]" : "[]")}");
                sb.AppendLine("        ParentFont = False");
                sb.AppendLine($"        {SizeValues(item.Height, item.Left, item.Top, item.Width).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
                sb.AppendLine("      end");
                continue;
            }

            if (type.Equals("Line", StringComparison.OrdinalIgnoreCase) || type.Equals("Rect", StringComparison.OrdinalIgnoreCase))
            {
                shapeNo++;
                var name = $"QRShape{shapeNo}";
                components.Add(new ComponentRef(name, "TQRShape"));
                sb.AppendLine($"      object {name}: TQRShape");
                sb.AppendLine($"        Left = {item.Left}");
                sb.AppendLine($"        Top = {item.Top}");
                sb.AppendLine($"        Width = {item.Width}");
                sb.AppendLine($"        Height = {item.Height}");
                sb.AppendLine($"        Pen.Width = {Math.Max(1, item.Thickness.GetValueOrDefault(1))}");
                sb.AppendLine($"        Shape = {MapShape(type, item.Orientation)}");
                sb.AppendLine($"        {SizeValues(item.Height, item.Left, item.Top, item.Width).Replace(Environment.NewLine, Environment.NewLine + "        ", StringComparison.Ordinal)}");
                sb.AppendLine("      end");
                continue;
            }

            if (type.Equals("Image", StringComparison.OrdinalIgnoreCase))
            {
                imageNo++;
                var name = $"QRImage{imageNo}";
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

    public string GeneratePas(string formName, string className, IReadOnlyCollection<ComponentRef> components)
    {
        var sb = new StringBuilder();
        sb.AppendLine($"unit {formName};");
        sb.AppendLine();
        sb.AppendLine("interface");
        sb.AppendLine();
        sb.AppendLine("uses");
        sb.AppendLine("  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms, Dialogs,");
        sb.AppendLine("  QuickRpt, QRCtrls, ExtCtrls;");
        sb.AppendLine();
        sb.AppendLine("type");
        sb.AppendLine($"  T{className} = class(TForm)");
        foreach (var component in components)
        {
            sb.AppendLine($"    {component.Name}: {component.Type};");
        }

        sb.AppendLine("  private");
        sb.AppendLine("  public");
        sb.AppendLine("  end;");
        sb.AppendLine();
        sb.AppendLine("var");
        sb.AppendLine($"  {formName}: T{className};");
        sb.AppendLine();
        sb.AppendLine("implementation");
        sb.AppendLine();
        sb.AppendLine("{$R *.dfm}");
        sb.AppendLine();
        sb.Append("end.");
        return sb.ToString();
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
}

public sealed record ComponentRef(string Name, string Type);
