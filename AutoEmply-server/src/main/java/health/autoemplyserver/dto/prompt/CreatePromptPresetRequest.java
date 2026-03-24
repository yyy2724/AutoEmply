package health.autoemplyserver.dto.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;

public record CreatePromptPresetRequest(
    @NotBlank String name,
    @NotBlank String systemPrompt,
    @NotBlank String userPromptTemplate,
    String styleRulesJson,
    List<UUID> sampleTemplateIds,
    String model,
    BigDecimal temperature,
    Integer maxTokens,
    @JsonProperty("isActive")
    boolean isActive,
    @JsonProperty("isPrimary")
    boolean isPrimary
) {
}
