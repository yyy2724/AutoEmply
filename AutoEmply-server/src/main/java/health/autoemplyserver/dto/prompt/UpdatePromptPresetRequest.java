package health.autoemplyserver.dto.prompt;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias("active")
    boolean isActive
) {
}
