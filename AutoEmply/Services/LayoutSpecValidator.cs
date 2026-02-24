using AutoEmply.Models;

namespace AutoEmply.Services;

public static class LayoutSpecValidator
{
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

        for (var i = 0; i < layoutSpec.Items.Count; i++)
        {
            var item = layoutSpec.Items[i];
            var path = $"layoutSpec.items[{i}]";

            if (string.IsNullOrWhiteSpace(item.Type))
            {
                errors.Add($"{path}.type is required.");
                continue;
            }

            var normalizedType = item.Type.Trim().ToLowerInvariant();
            if (normalizedType is not ("text" or "line" or "rect" or "image"))
            {
                errors.Add($"{path}.type must be one of Text, Line, Rect, Image.");
            }

            if (item.Width < 0 || item.Height < 0 || item.Left < 0 || item.Top < 0)
            {
                errors.Add($"{path} has invalid coordinates/size. left/top/width/height must be >= 0.");
            }
        }

        return errors;
    }
}
