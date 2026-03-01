using AutoEmply.Models;
using System.Text.RegularExpressions;

namespace AutoEmply.Services;

public static class LayoutSpecValidator
{
    private static readonly Regex HexColorRegex = new("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$", RegexOptions.Compiled);
    private static readonly Regex DelphiHexColorRegex = new("^\\$[0-9A-Fa-f]{8}$", RegexOptions.Compiled);
    private static readonly Regex DelphiColorNameRegex = new("^cl[A-Za-z0-9_]+$", RegexOptions.Compiled);

    public static List<string> Validate(string formName, LayoutSpec? layoutSpec)
    {
        var errors = new List<string>();

        if (string.IsNullOrWhiteSpace(formName))
        {
            errors.Add("formName is required.");
        }
        else if (string.IsNullOrWhiteSpace(formName.Replace("_", string.Empty, StringComparison.Ordinal)))
        {
            errors.Add("formName must include at least one non-underscore character.");
        }

        if (layoutSpec is null)
        {
            errors.Add("layoutSpec is required.");
            return errors;
        }

        if (layoutSpec.Items is null || layoutSpec.Items.Count == 0)
        {
            errors.Add("layoutSpec.items must contain at least one item.");
            return errors;
        }

        var duplicates = new HashSet<string>(StringComparer.Ordinal);
        var duplicateCount = 0;
        var tinyTextCount = 0;

        for (var i = 0; i < layoutSpec.Items.Count; i++)
        {
            var item = layoutSpec.Items[i];
            var path = $"layoutSpec.items[{i}]";

            if (string.IsNullOrWhiteSpace(item.Type))
            {
                errors.Add($"{path}.type is required.");
                continue;
            }

            var normalizedType = NormalizeItemType(item.Type);
            if (string.IsNullOrEmpty(normalizedType))
            {
                errors.Add($"{path}.type must be one of Text, Line, Rect, Image.");
                continue;
            }

            if (item.Width < 0 || item.Height < 0 || item.Left < 0 || item.Top < 0)
            {
                errors.Add($"{path} has invalid coordinates/size. left/top/width/height must be >= 0.");
            }

            if (item.Width == 0 || item.Height == 0)
            {
                errors.Add($"{path} has zero size. width/height must be > 0.");
            }

            if (normalizedType == "line")
            {
                var isHorizontal = string.Equals(item.Orientation, "H", StringComparison.OrdinalIgnoreCase);
                var isVertical = string.Equals(item.Orientation, "V", StringComparison.OrdinalIgnoreCase);
                var inferableByGeometry = item.Width != item.Height;
                if (!isHorizontal && !isVertical && !inferableByGeometry)
                {
                    errors.Add($"{path}.orientation must be H or V for line items.");
                }
            }

            if (normalizedType == "text")
            {
                var captionLength = (item.Caption ?? string.Empty).Trim().Length;
                if (captionLength > 0 && item.Width < 10 && item.Height < 10)
                {
                    tinyTextCount++;
                }
            }

            var key = string.Join("|",
                normalizedType,
                item.Left,
                item.Top,
                item.Width,
                item.Height,
                item.Caption ?? string.Empty,
                item.Align ?? string.Empty,
                item.FontSize,
                item.Bold,
                item.Transparent,
                item.TextColor ?? string.Empty,
                item.Orientation ?? string.Empty,
                item.Thickness,
                item.StrokeColor ?? string.Empty,
                item.FillColor ?? string.Empty,
                item.Filled,
                item.Stretch);
            if (!duplicates.Add(key))
            {
                duplicateCount++;
            }

            if (!IsValidColorOrEmpty(item.TextColor))
            {
                errors.Add($"{path}.textColor must be #RRGGBB/#AARRGGBB/$00BBGGRR/or Delphi color name (clXxx).");
            }

            if (!IsValidColorOrEmpty(item.StrokeColor))
            {
                errors.Add($"{path}.strokeColor must be #RRGGBB/#AARRGGBB/$00BBGGRR/or Delphi color name (clXxx).");
            }

            if (!IsValidColorOrEmpty(item.FillColor))
            {
                errors.Add($"{path}.fillColor must be #RRGGBB/#AARRGGBB/$00BBGGRR/or Delphi color name (clXxx).");
            }

        }

        var duplicateLimit = Math.Max(20, (int)Math.Round(layoutSpec.Items.Count * 0.4, MidpointRounding.AwayFromZero));
        if (duplicateCount > duplicateLimit)
        {
            errors.Add($"layoutSpec contains too many duplicate items ({duplicateCount}).");
        }

        var tinyTextLimit = Math.Max(30, (int)Math.Round(layoutSpec.Items.Count * 0.45, MidpointRounding.AwayFromZero));
        if (tinyTextCount > tinyTextLimit)
        {
            errors.Add($"layoutSpec contains too many tiny text boxes ({tinyTextCount}). This usually indicates over-segmentation.");
        }

        if (layoutSpec.Pas is not null)
        {
            if (layoutSpec.Pas.Uses is not null)
            {
                for (var i = 0; i < layoutSpec.Pas.Uses.Count; i++)
                {
                    var unitName = layoutSpec.Pas.Uses[i]?.Trim() ?? string.Empty;
                    if (string.IsNullOrWhiteSpace(unitName))
                    {
                        errors.Add($"layoutSpec.pas.uses[{i}] must not be empty.");
                    }
                }
            }

            if (layoutSpec.Pas.Methods is not null)
            {
                for (var i = 0; i < layoutSpec.Pas.Methods.Count; i++)
                {
                    var method = layoutSpec.Pas.Methods[i];
                    var path = $"layoutSpec.pas.methods[{i}]";
                    if (string.IsNullOrWhiteSpace(method.Declaration))
                    {
                        errors.Add($"{path}.declaration is required.");
                    }

                    if (method.Body is null || method.Body.Count == 0)
                    {
                        errors.Add($"{path}.body must contain at least one line.");
                    }
                }
            }
        }

        return errors;
    }

    private static bool IsValidColorOrEmpty(string? value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return true;
        }

        var color = value.Trim();
        return HexColorRegex.IsMatch(color) ||
               DelphiHexColorRegex.IsMatch(color) ||
               DelphiColorNameRegex.IsMatch(color);
    }

    private static string NormalizeItemType(string? rawType)
    {
        if (string.IsNullOrWhiteSpace(rawType))
        {
            return string.Empty;
        }

        var compact = new string(rawType
            .Trim()
            .ToLowerInvariant()
            .Where(char.IsLetterOrDigit)
            .ToArray());

        return compact switch
        {
            "text" or "label" or "caption" or "memo" or "string" => "text",
            "line" or "hline" or "vline" or "horline" or "vertline" or "horizontalline" or "verticalline" => "line",
            "rect" or "rectangle" or "box" or "cell" or "tablecell" => "rect",
            "image" or "img" or "picture" or "photo" or "logo" => "image",
            _ => string.Empty
        };
    }
}
