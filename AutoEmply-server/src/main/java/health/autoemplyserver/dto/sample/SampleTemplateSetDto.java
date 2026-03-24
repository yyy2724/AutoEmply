package health.autoemplyserver.dto.sample;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SampleTemplateSetDto(
    UUID id,
    String name,
    List<UUID> templateIds,
    boolean isActive,
    boolean isPrimary,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
