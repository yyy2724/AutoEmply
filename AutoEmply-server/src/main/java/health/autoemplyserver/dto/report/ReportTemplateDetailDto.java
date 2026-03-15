package health.autoemplyserver.dto.report;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportTemplateDetailDto(
    UUID id,
    String name,
    String category,
    String originalFormName,
    String dfmContent,
    String pasContent,
    boolean hasPreview,
    String previewContentType,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
