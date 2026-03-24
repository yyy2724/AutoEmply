package health.autoemplyserver.service;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.delphi.DelphiComponentWriter;
import health.autoemplyserver.service.delphi.DelphiComponentWriter.ComponentCounters;
import health.autoemplyserver.service.delphi.DelphiPasWriter;
import health.autoemplyserver.service.delphi.DelphiValueFormatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class DelphiGenerator {

    private static final int QUICK_REP_WIDTH = 794;
    private static final int MAX_BAND_WIDTH = 794;
    private static final int MAX_BAND_HEIGHT = 6000;
    private static final int BAND_PADDING = 2;
    private static final int MIN_FONT_SIZE = 6;
    private static final int MAX_FONT_SIZE = 24;
    private static final int MAX_PEN_THICKNESS = 6;

    private final DelphiValueFormatter formatter;
    private final DelphiComponentWriter componentWriter;
    private final DelphiPasWriter pasWriter;

    public DelphiGenerator(
        DelphiValueFormatter formatter,
        DelphiComponentWriter componentWriter,
        DelphiPasWriter pasWriter
    ) {
        this.formatter = formatter;
        this.componentWriter = componentWriter;
        this.pasWriter = pasWriter;
    }

    public byte[] generateZip(String formName, LayoutSpec layoutSpec) {
        String className = formName.replace("_", "");
        List<ComponentRef> components = new ArrayList<>();
        String dfm = generateDfm(formName, className, layoutSpec, components);
        String pas = pasWriter.write(formName, className, components, layoutSpec.getPas());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                writeEntry(zipOutputStream, formName + ".dfm", dfm);
                writeEntry(zipOutputStream, formName + ".pas", pas);
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build Delphi archive.", exception);
        }
    }

    public String generateDfm(String formName, String className, LayoutSpec layoutSpec, List<ComponentRef> components) {
        components.clear();
        components.add(new ComponentRef("QuickRep1", "TQuickRep"));
        components.add(new ComponentRef("DetailBand1", "TQRBand"));

        List<LayoutItem> items = prepareItems(layoutSpec.getItems());
        int bandWidth = calculateBandWidth(items);
        int bandHeight = calculateBandHeight(items);
        items = clampItemsToBand(items, bandWidth, bandHeight);

        StringBuilder builder = new StringBuilder();
        builder.append("object ").append(className).append(": T").append(className).append('\n')
            .append("  Left = 0\n")
            .append("  Top = 0\n")
            .append("  Width = 800\n")
            .append("  Height = 600\n")
            .append("  Caption = ").append(formatter.encodeString(formName)).append('\n')
            .append("  OldCreateOrder = False\n")
            .append("  PixelsPerInch = 96\n")
            .append("  TextHeight = 13\n")
            .append("  object QuickRep1: TQuickRep\n")
            .append("    Left = 0\n")
            .append("    Top = 0\n")
            .append("    Width = ").append(QUICK_REP_WIDTH).append('\n')
            .append("    Height = 1123\n")
            .append("    DataSet = nil\n")
            .append("    Functions.Strings = (\n")
            .append("      'PAGENUMBER'\n")
            .append("      'COLUMNNUMBER'\n")
            .append("      'REPORTTITLE'\n")
            .append("      'QRSTRINGSBAND1'\n")
            .append("      'QRSTRINGSBAND2'\n")
            .append("      'QRSTRINGSBAND3'\n")
            .append("      'QRSTRINGSBAND4'\n")
            .append("      'QRSTRINGSBAND5'\n")
            .append("      'QRSTRINGSBAND6'\n")
            .append("      'QRSTRINGSBAND7')\n")
            .append("    Functions.DATA = (\n")
            .append("      '0'\n")
            .append("      '0'\n")
            .append("      ''''''\n")
            .append("      ''\n")
            .append("      ''\n")
            .append("      ''\n")
            .append("      ''\n")
            .append("      ''\n")
            .append("      ''\n")
            .append("      '')\n")
            .append("    Options = [FirstPageHeader, LastPageFooter]\n")
            .append("    Page.Columns = 1\n")
            .append("    Page.Orientation = poPortrait\n")
            .append("    Page.PaperSize = A4\n")
            .append("    Page.Values = (\n")
            .append("      100.000000000000000000\n")
            .append("      2970.000000000000000000\n")
            .append("      100.000000000000000000\n")
            .append("      2100.000000000000000000\n")
            .append("      100.000000000000000000\n")
            .append("      100.000000000000000000\n")
            .append("      0.000000000000000000)\n")
            .append("    PrinterSettings.Copies = 1\n")
            .append("    PrinterSettings.OutputBin = Auto\n")
            .append("    PrinterSettings.Duplex = False\n")
            .append("    PrinterSettings.FirstPage = 0\n")
            .append("    PrinterSettings.LastPage = 0\n")
            .append("    PrinterSettings.UseStandardprinter = False\n")
            .append("    PrinterSettings.UseCustomBinCode = False\n")
            .append("    object DetailBand1: TQRBand\n")
            .append("      Left = 0\n")
            .append("      Top = 38\n")
            .append("      Width = ").append(bandWidth).append('\n')
            .append("      Height = ").append(bandHeight).append('\n')
            .append("      BandType = rbDetail\n")
            .append("      ").append(formatter.bandSizeValues(bandHeight, bandWidth).replace("\n", "\n      ")).append('\n');

        appendComponents(builder, items, components);

        builder.append("    end\n")
            .append("  end\n")
            .append("end");
        return builder.toString();
    }

    public String encodeDelphi(String text) {
        return formatter.encodeString(text);
    }

    private void appendComponents(StringBuilder builder, List<LayoutItem> items, List<ComponentRef> components) {
        ComponentCounters counters = new ComponentCounters();
        Set<String> usedNames = new HashSet<>();
        for (LayoutItem item : items) {
            if ("Text".equalsIgnoreCase(item.getType())) {
                componentWriter.writeLabel(builder, item, components, counters, usedNames);
            } else if ("Line".equalsIgnoreCase(item.getType()) || "Rect".equalsIgnoreCase(item.getType())) {
                componentWriter.writeShape(builder, item, components, counters, usedNames);
            } else if ("Image".equalsIgnoreCase(item.getType())) {
                componentWriter.writeImage(builder, item, components, counters, usedNames);
            }
        }
    }

    private List<LayoutItem> prepareItems(List<LayoutItem> items) {
        if (items == null) {
            return List.of();
        }
        List<LayoutItem> normalized = items.stream()
            .map(LayoutItem::copy)
            .filter(item -> normalizeType(item.getType()) != null)
            .peek(item -> {
                String originalType = item.getType();
                String normalizedType = normalizeType(item.getType());
                item.setType(normalizedType);
                item.setCaption(item.getCaption() == null ? null : item.getCaption().trim());
                item.setLeft(Math.max(0, item.getLeft()));
                item.setTop(Math.max(0, item.getTop()));
                item.setWidth(Math.max(1, item.getWidth()));
                item.setHeight(Math.max(1, item.getHeight()));
                item.setFontSize(clampNullable(item.getFontSize(), 10, MIN_FONT_SIZE, MAX_FONT_SIZE));
                item.setThickness(clampNullable(item.getThickness(), 1, 1, MAX_PEN_THICKNESS));
                if ("Line".equalsIgnoreCase(normalizedType)) {
                    item.setOrientation(inferLineOrientation(item, originalType, item.getOrientation()));
                }
            })
            .sorted(Comparator.comparingInt(LayoutItem::getTop).thenComparingInt(LayoutItem::getLeft))
            .toList();

        List<LayoutItem> deduplicated = deduplicateItems(normalized);
        return clampItemsToWidth(deduplicated, MAX_BAND_WIDTH);
    }

    private int calculateBandWidth(List<LayoutItem> items) {
        return Math.max(
            64,
            Math.min(
                MAX_BAND_WIDTH,
                items.stream().mapToInt(item -> item.getLeft() + item.getWidth()).max().orElse(MAX_BAND_WIDTH - BAND_PADDING) + BAND_PADDING
            )
        );
    }

    private int calculateBandHeight(List<LayoutItem> items) {
        return Math.max(
            32,
            Math.min(
                MAX_BAND_HEIGHT,
                items.stream().mapToInt(item -> item.getTop() + item.getHeight()).max().orElse(1041) + BAND_PADDING
            )
        );
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        String compact = compactString(rawType);
        return switch (compact) {
            case "text", "label", "caption", "memo", "string" -> "Text";
            case "line", "hline", "vline", "horline", "vertline", "horizontalline", "verticalline" -> "Line";
            case "rect", "rectangle", "box", "cell", "tablecell" -> "Rect";
            case "image", "img", "picture", "photo", "logo" -> "Image";
            default -> null;
        };
    }

    private List<LayoutItem> deduplicateItems(List<LayoutItem> items) {
        if (items.size() <= 1) {
            return items;
        }

        Set<String> seen = new HashSet<>();
        List<LayoutItem> result = new ArrayList<>();
        for (LayoutItem item : items) {
            String key = String.join("|",
                Objects.toString(item.getType(), ""),
                String.valueOf(item.getLeft()),
                String.valueOf(item.getTop()),
                String.valueOf(item.getWidth()),
                String.valueOf(item.getHeight()),
                Objects.toString(item.getOrientation(), ""),
                Objects.toString(item.getCaption(), ""),
                Objects.toString(item.getAlign(), ""),
                Objects.toString(item.getFontSize(), ""),
                Objects.toString(item.getBold(), ""),
                Objects.toString(item.getTransparent(), ""),
                Objects.toString(item.getTextColor(), ""),
                Objects.toString(item.getThickness(), ""),
                Objects.toString(item.getStrokeColor(), ""),
                Objects.toString(item.getFillColor(), ""),
                Objects.toString(item.getFilled(), ""),
                Objects.toString(item.getStretch(), "")
            );
            if (seen.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    private List<LayoutItem> clampItemsToWidth(List<LayoutItem> items, int maxWidth) {
        if (items.isEmpty()) {
            return items;
        }

        int currentMaxRight = items.stream()
            .mapToInt(item -> Math.max(0, item.getLeft()) + Math.max(1, item.getWidth()))
            .max()
            .orElse(0);
        if (currentMaxRight <= maxWidth) {
            return items;
        }

        return items.stream()
            .map(item -> {
                LayoutItem copy = item.copy();
                copy.setLeft(Math.clamp(copy.getLeft(), 0, maxWidth - 1));
                copy.setWidth(Math.max(1, Math.min(copy.getWidth(), maxWidth - copy.getLeft())));
                return copy;
            })
            .toList();
    }

    private List<LayoutItem> clampItemsToBand(List<LayoutItem> items, int bandWidth, int bandHeight) {
        if (items.isEmpty()) {
            return items;
        }

        return items.stream()
            .map(item -> {
                LayoutItem copy = item.copy();
                copy.setLeft(Math.clamp(copy.getLeft(), 0, Math.max(1, bandWidth) - 1));
                copy.setTop(Math.clamp(copy.getTop(), 0, Math.max(1, bandHeight) - 1));
                copy.setWidth(Math.max(1, Math.min(copy.getWidth(), bandWidth - copy.getLeft())));
                copy.setHeight(Math.max(1, Math.min(copy.getHeight(), bandHeight - copy.getTop())));
                return copy;
            })
            .toList();
    }

    private Integer clampNullable(Integer value, int fallback, int min, int max) {
        int resolved = value == null ? fallback : value;
        return Math.clamp(resolved, min, max);
    }

    private String inferLineOrientation(LayoutItem item, String originalType, String originalOrientation) {
        String compact = compactString(originalType);
        if (compact.contains("vline") || compact.contains("vert")) {
            return "V";
        }
        if (compact.contains("hline") || compact.contains("hor")) {
            return "H";
        }
        if (originalOrientation != null && originalOrientation.trim().equalsIgnoreCase("V")) {
            return "V";
        }
        return item.getHeight() > item.getWidth() ? "V" : "H";
    }

    private String compactString(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
        writer.write(content);
        writer.flush();
        zipOutputStream.closeEntry();
    }
}
