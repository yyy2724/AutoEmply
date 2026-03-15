package health.autoemplyserver.dto.report;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportTemplateSummaryDto(
    UUID id,
    String name,
    String category,
    String originalFormName,
    boolean hasPreview,
    String previewContentType,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
