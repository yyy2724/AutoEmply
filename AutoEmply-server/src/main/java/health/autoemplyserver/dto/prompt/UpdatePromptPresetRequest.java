package health.autoemplyserver.dto.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;

public record UpdatePromptPresetRequest(
    @NotBlank String name,
    @NotBlank String systemPrompt,
    @NotBlank String userPromptTemplate,
    String styleRulesJson,
    String model,
    BigDecimal temperature,
    Integer maxTokens,
    @JsonProperty("isActive")
    boolean isActive,
    @JsonProperty("isPrimary")
    boolean isPrimary
) {
}
