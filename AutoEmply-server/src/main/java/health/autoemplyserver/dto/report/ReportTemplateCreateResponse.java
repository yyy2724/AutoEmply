package health.autoemplyserver.dto.report;

import java.util.UUID;

public record ReportTemplateCreateResponse(
    UUID id,
    String name,
    String category,
    String originalFormName
) {
}
