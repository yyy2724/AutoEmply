package health.autoemplyserver.dto.sample;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record UpdateSampleTemplateSetRequest(
    @NotBlank String name,
    List<UUID> templateIds,
    @JsonProperty("isActive") boolean isActive,
    @JsonProperty("isPrimary") boolean isPrimary
) {
}
