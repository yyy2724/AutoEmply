package health.autoemplyserver.dto.sample;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SampleTemplateSetDto(
    UUID id,
    String name,
    List<UUID> templateIds,
    @JsonProperty("isActive") boolean isActive,
    @JsonProperty("isPrimary") boolean isPrimary,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
