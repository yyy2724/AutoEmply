package health.autoemplyserver.dto.sample;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateSampleTemplateSetRequest(
    @NotBlank String name,
    List<UUID> templateIds,
    @JsonAlias("active") boolean isActive
) {
}
