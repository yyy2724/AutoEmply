package health.autoemplyserver.dto.prompt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PromptPresetDto(
    UUID id,
    String name,
    String systemPrompt,
    String userPromptTemplate,
    String styleRulesJson,
    List<UUID> sampleTemplateIds,
    String model,
    BigDecimal temperature,
    Integer maxTokens,
    boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
