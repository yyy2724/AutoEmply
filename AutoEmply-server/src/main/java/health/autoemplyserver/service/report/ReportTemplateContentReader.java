package health.autoemplyserver.service.report;

import health.autoemplyserver.support.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ReportTemplateContentReader {

    private static final Charset DFM_CHARSET = Charset.forName("MS949");
    private static final long MAX_PREVIEW_BYTES = 10L * 1024 * 1024;

    public String readRequiredText(MultipartFile file, String label) {
        String content = readText(file);
        if (content == null || content.isBlank()) {
            throw new BadRequestException(label + " file is required.");
        }
        return content;
    }

    public String inferOriginalFormName(MultipartFile dfmFile) {
        String originalFileName = dfmFile == null ? null : dfmFile.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BadRequestException("Unable to infer original form name from DFM file.");
        }

        String formName = originalFileName.replaceFirst("\\.[^.]+$", "").trim();
        if (formName.isBlank()) {
            throw new BadRequestException("Unable to infer original form name from DFM file.");
        }
        return formName;
    }

    public PreviewContent readPreview(MultipartFile previewFile) {
        if (previewFile == null || previewFile.isEmpty()) {
            return PreviewContent.empty();
        }
        if (previewFile.getSize() > MAX_PREVIEW_BYTES) {
            throw new BadRequestException("Preview file exceeds 10MB limit.");
        }
        try {
            return new PreviewContent(previewFile.getBytes(), previewFile.getContentType());
        } catch (IOException exception) {
            throw new BadRequestException("Failed to read preview file.", exception);
        }
    }

    private String readText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(file.getInputStream(), DFM_CHARSET)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException exception) {
            throw new BadRequestException("Failed to read uploaded file.", exception);
        }
    }

    public record PreviewContent(byte[] data, String contentType) {

        public static PreviewContent empty() {
            return new PreviewContent(null, null);
        }
    }
}
