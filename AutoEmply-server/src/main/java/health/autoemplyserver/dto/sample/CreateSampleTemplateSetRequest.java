package health.autoemplyserver.dto.sample;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateSampleTemplateSetRequest(
    @NotBlank String name,
    List<UUID> templateIds
) {
}
