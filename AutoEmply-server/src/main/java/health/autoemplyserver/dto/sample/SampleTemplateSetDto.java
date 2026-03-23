package health.autoemplyserver.dto.sample;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SampleTemplateSetDto(
    UUID id,
    String name,
    List<UUID> templateIds,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
