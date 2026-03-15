package health.autoemplyserver.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class DelphiRenamer {

    private static final Charset EUC_KR = Charset.forName("MS949");
    private static final Pattern FORM_NAME_PATTERN = Pattern.compile("^object\\s+(\\w+)\\s*:", Pattern.CASE_INSENSITIVE);

    public byte[] renameAndZip(
        String originalFileName,
        String dfmInternalName,
        String newFormName,
        String dfmContent,
        String pasContent
    ) {
        String newDfmName = newFormName.replace("_", "");
        String oldClassName = "T" + dfmInternalName.replace("_", "");
        String newClassName = "T" + newDfmName;

        String renamedDfm = renameContent(dfmContent, originalFileName, dfmInternalName, newFormName, newDfmName, oldClassName, newClassName);
        String renamedPas = renameContent(pasContent, originalFileName, dfmInternalName, newFormName, newDfmName, oldClassName, newClassName);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                writeEntry(zipOutputStream, newFormName + ".dfm", renamedDfm);
                writeEntry(zipOutputStream, newFormName + ".pas", renamedPas);
            }
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build Delphi zip.", exception);
        }
    }

    public String extractFormNameFromDfm(String dfmContent) {
        if (dfmContent == null || dfmContent.isBlank()) {
            return null;
        }
        String firstLine = dfmContent.lines().findFirst().orElse("").trim();
        Matcher matcher = FORM_NAME_PATTERN.matcher(firstLine);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String renameContent(
        String content,
        String originalFileName,
        String dfmInternalName,
        String newFormName,
        String newDfmName,
        String oldClassName,
        String newClassName
    ) {
        String renamed = replaceExact(content, oldClassName, newClassName);
        if (!originalFileName.equals(dfmInternalName)) {
            renamed = replaceExact(renamed, originalFileName, newFormName);
            return replaceExact(renamed, dfmInternalName, newDfmName);
        }
        return replaceExact(renamed, originalFileName, newFormName);
    }

    private String replaceExact(String input, String oldValue, String newValue) {
        if (oldValue == null || oldValue.isBlank() || oldValue.equals(newValue)) {
            return input;
        }
        return input.replaceAll("\\b" + Pattern.quote(oldValue) + "\\b", Matcher.quoteReplacement(newValue));
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        OutputStreamWriter writer = new OutputStreamWriter(zipOutputStream, EUC_KR);
        writer.write(content);
        writer.flush();
        zipOutputStream.closeEntry();
    }
}
