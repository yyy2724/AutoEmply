package health.autoemplyserver.dto.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromptPresetDto(
    UUID id,
    String name,
    String systemPrompt,
    String userPromptTemplate,
    String styleRulesJson,
    String model,
    BigDecimal temperature,
    Integer maxTokens,
    @JsonProperty("isActive") boolean isActive,
    @JsonProperty("isPrimary") boolean isPrimary,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
