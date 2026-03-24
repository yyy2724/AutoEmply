package health.autoemplyserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import health.autoemplyserver.model.LayoutItem;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.model.PasMethodSpec;
import health.autoemplyserver.model.PasSpec;
import health.autoemplyserver.service.delphi.DelphiComponentWriter;
import health.autoemplyserver.service.delphi.DelphiPasWriter;
import health.autoemplyserver.service.delphi.DelphiValueFormatter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class DelphiGeneratorRegressionTest {

    private final DelphiValueFormatter formatter = new DelphiValueFormatter();
    private final DelphiGenerator generator = new DelphiGenerator(
        formatter,
        new DelphiComponentWriter(formatter),
        new DelphiPasWriter(formatter)
    );

    @Test
    void generatesDelphiOutputWithSameCorrectionsAsCSharpReference() throws IOException {
        LayoutSpec spec = new LayoutSpec();
        spec.setItems(sampleItems());
        spec.setPas(samplePasSpec());

        byte[] zipBytes = generator.generateZip("Form_QREmply25", spec);
        Map<String, String> entries = unzipAsciiEntries(zipBytes);

        assertThat(entries).containsOnlyKeys("Form_QREmply25.dfm", "Form_QREmply25.pas");

        String dfm = entries.get("Form_QREmply25.dfm");
        assertThat(dfm).contains("DataSet = nil");
        assertThat(dfm).contains("Functions.Strings = (");
        assertThat(dfm).contains("PrinterSettings.UseCustomBinCode = False");
        assertThat(dfm).contains("Width = 794");
        assertThat(dfm).contains("Height = 6000");
        assertThat(dfm).contains("object Header_Title: TQRLabel");
        assertThat(dfm).contains("Caption = #49444#47749");
        assertThat(dfm).doesNotContain("DuplicateLabel");
        assertThat(dfm).contains("object QRShape_123bad_name: TQRShape");
        assertThat(dfm).contains("Shape = qrsVertLine");
        assertThat(dfm).contains("Pen.Width = 6");
        assertThat(dfm).contains("Brush.Style = bsSolid");
        assertThat(dfm).contains("Brush.Color = $0000FF00");
        assertThat(dfm).contains("object QRImage1: TQRImage");
        assertThat(dfm).contains("Stretch = True");

        String pas = entries.get("Form_QREmply25.pas");
        assertThat(pas).contains("unit Form_QREmply25;");
        assertThat(pas).contains("Forms,");
        assertThat(pas).contains("MyUnit;");
        assertThat(pas).contains("procedure BuildReport;");
        assertThat(pas).contains("function CalcValue: Integer;");
        assertThat(pas).contains("procedure TFormQREmply25.BuildReport;");
        assertThat(pas).contains("function TFormQREmply25.CalcValue: Integer;");
        assertThat(pas).contains("// 한글 주석 유지");
        assertThat(pas).contains("var\n  counter: Integer;\nbegin\n  counter := 1;");
        assertThat(pas).contains("// TODO");
        assertThat(pas).doesNotContain("begin\n  begin");
        assertThat(pas).doesNotContain("begin\n  var");
        assertThat(pas).doesNotContain("Header_TitlePrint");
    }

    private List<LayoutItem> sampleItems() {
        LayoutItem label = new LayoutItem();
        label.setName("Header_Title");
        label.setType("label");
        label.setLeft(-10);
        label.setTop(5);
        label.setWidth(140);
        label.setHeight(24);
        label.setCaption("설명");
        label.setAlign("center");
        label.setFontSize(40);
        label.setBold(true);
        label.setTransparent(false);
        label.setTextColor("#112233");
        LayoutItem duplicate = label.copy();
        duplicate.setName("DuplicateLabel");

        LayoutItem line = new LayoutItem();
        line.setName("123bad name");
        line.setType("vertical-line");
        line.setLeft(900);
        line.setTop(6100);
        line.setWidth(4);
        line.setHeight(12);
        line.setThickness(10);
        line.setStrokeColor("#FF0000");

        LayoutItem rect = new LayoutItem();
        rect.setType("box");
        rect.setLeft(760);
        rect.setTop(5900);
        rect.setWidth(100);
        rect.setHeight(200);
        rect.setFillColor("#00FF00");
        rect.setFilled(false);

        LayoutItem image = new LayoutItem();
        image.setType("photo");
        image.setLeft(20);
        image.setTop(80);
        image.setWidth(180);
        image.setHeight(90);

        return List.of(label, duplicate, line, rect, image);
    }

    private PasSpec samplePasSpec() {
        PasMethodSpec buildReport = new PasMethodSpec();
        buildReport.setDeclaration("procedure BuildReport");
        buildReport.setBody(List.of("// 한글 주석 유지", "var", "  counter: Integer;", "counter := 1;", "ShowMessage('ready');"));

        PasMethodSpec calcValue = new PasMethodSpec();
        calcValue.setDeclaration("function CalcValue: Integer;");
        calcValue.setBody(new ArrayList<>());

        PasSpec pasSpec = new PasSpec();
        pasSpec.setUses(List.of("MyUnit;", "Forms"));
        pasSpec.setMethods(List.of(buildReport, calcValue));
        return pasSpec;
    }

    private Map<String, String> unzipAsciiEntries(byte[] zipBytes) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            while (true) {
                var entry = zipInputStream.getNextEntry();
                if (entry == null) {
                    return entries;
                }
                entries.put(entry.getName(), new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }
}
