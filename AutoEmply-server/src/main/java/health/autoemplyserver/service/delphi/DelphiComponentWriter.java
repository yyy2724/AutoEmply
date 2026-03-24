package health.autoemplyserver.service.delphi;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.service.ComponentRef;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DelphiComponentWriter {

    private final DelphiValueFormatter formatter;

    public DelphiComponentWriter(DelphiValueFormatter formatter) {
        this.formatter = formatter;
    }

    public void writeLabel(StringBuilder builder, LayoutItem item, List<ComponentRef> components, ComponentCounters counters, Set<String> usedNames) {
        String name = buildComponentName(usedNames, "QRLabel", item.getName(), ++counters.label);
        components.add(new ComponentRef(name, "TQRLabel"));
        builder.append("      object ").append(name).append(": TQRLabel\n")
            .append("        Left = ").append(item.getLeft()).append('\n')
            .append("        Top = ").append(item.getTop()).append('\n')
            .append("        Width = ").append(item.getWidth()).append('\n')
            .append("        Height = ").append(item.getHeight()).append('\n')
            .append("        Alignment = ").append(formatter.mapAlignment(item.getAlign())).append('\n')
            .append("        Caption = ").append(formatter.encodeString(item.getCaption() == null ? "" : item.getCaption())).append('\n')
            .append("        AutoSize = False\n")
            .append("        Transparent = ").append(Boolean.TRUE.equals(item.getTransparent()) ? "True" : "False").append('\n')
            .append("        WordWrap = True\n")
            .append("        Font.Charset = DEFAULT_CHARSET\n")
            .append("        Font.Color = ").append(formatter.toDelphiColor(item.getTextColor(), "clBlack")).append('\n')
            .append("        Font.Name = 'Gulim'\n")
            .append("        Font.Size = ").append(item.getFontSize() == null ? 10 : Math.max(6, Math.min(24, item.getFontSize()))).append('\n')
            .append("        Font.Style = ").append(Boolean.TRUE.equals(item.getBold()) ? "[fsBold]" : "[]").append('\n')
            .append("        ParentFont = False\n");
        builder.append("        ").append(formatter.itemSizeValues(item.getLeft(), item.getTop(), item.getWidth(), item.getHeight()).replace("\n", "\n        ")).append('\n')
            .append("      end\n");
    }

    public void writeShape(StringBuilder builder, LayoutItem item, List<ComponentRef> components, ComponentCounters counters, Set<String> usedNames) {
        String name = buildComponentName(usedNames, "QRShape", item.getName(), ++counters.shape);
        components.add(new ComponentRef(name, "TQRShape"));
        builder.append("      object ").append(name).append(": TQRShape\n")
            .append("        Left = ").append(item.getLeft()).append('\n')
            .append("        Top = ").append(item.getTop()).append('\n')
            .append("        Width = ").append(item.getWidth()).append('\n')
            .append("        Height = ").append(item.getHeight()).append('\n')
            .append("        Pen.Width = ").append(item.getThickness() == null ? 1 : Math.max(1, Math.min(6, item.getThickness()))).append('\n')
            .append("        Pen.Color = ").append(formatter.toDelphiColor(item.getStrokeColor(), "clBlack")).append('\n')
            .append("        Shape = ").append("Rect".equalsIgnoreCase(item.getType()) ? "qrsRectangle" : ("V".equalsIgnoreCase(item.getOrientation()) ? "qrsVertLine" : "qrsHorLine")).append('\n');
        if ("Rect".equalsIgnoreCase(item.getType())) {
            if (Boolean.TRUE.equals(item.getFilled()) || (item.getFillColor() != null && !item.getFillColor().isBlank())) {
                builder.append("        Brush.Style = bsSolid\n")
                    .append("        Brush.Color = ").append(formatter.toDelphiColor(item.getFillColor(), "clWhite")).append('\n');
            } else {
                builder.append("        Brush.Style = bsClear\n");
            }
        }
        builder.append("        ").append(formatter.itemSizeValues(item.getLeft(), item.getTop(), item.getWidth(), item.getHeight()).replace("\n", "\n        ")).append('\n')
            .append("      end\n");
    }

    public void writeImage(StringBuilder builder, LayoutItem item, List<ComponentRef> components, ComponentCounters counters, Set<String> usedNames) {
        String name = buildComponentName(usedNames, "QRImage", item.getName(), ++counters.image);
        components.add(new ComponentRef(name, "TQRImage"));
        builder.append("      object ").append(name).append(": TQRImage\n")
            .append("        Left = ").append(item.getLeft()).append('\n')
            .append("        Top = ").append(item.getTop()).append('\n')
            .append("        Width = ").append(item.getWidth()).append('\n')
            .append("        Height = ").append(item.getHeight()).append('\n')
            .append("        Stretch = ").append(Boolean.FALSE.equals(item.getStretch()) ? "False" : "True").append('\n')
            .append("        ").append(formatter.itemSizeValues(item.getLeft(), item.getTop(), item.getWidth(), item.getHeight()).replace("\n", "\n        ")).append('\n')
            .append("      end\n");
    }

    private String buildComponentName(Set<String> usedNames, String fallbackPrefix, String preferred, int counter) {
        String candidate = preferred == null || preferred.isBlank()
            ? fallbackPrefix + counter
            : preferred.trim().replaceAll("[^A-Za-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (candidate.isBlank()) {
            candidate = fallbackPrefix + counter;
        }
        if (candidate.length() > 48) {
            candidate = candidate.substring(0, 48);
        }
        if (!Character.isLetter(candidate.charAt(0)) && candidate.charAt(0) != '_') {
            candidate = fallbackPrefix + "_" + candidate;
        }
        if (usedNames.add(candidate)) {
            return candidate;
        }
        int suffix = 2;
        while (!usedNames.add(candidate + "_" + suffix)) {
            suffix++;
        }
        return candidate + "_" + suffix;
    }

    public static final class ComponentCounters {
        private int label;
        private int shape;
        private int image;
    }
}
