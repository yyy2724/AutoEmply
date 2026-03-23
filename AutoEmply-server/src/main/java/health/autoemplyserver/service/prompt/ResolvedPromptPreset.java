package health.autoemplyserver.service.prompt;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResolvedPromptPreset(
    UUID id,
    String name,
    String systemPrompt,
    String userPromptTemplate,
    String styleRulesJson,
    List<UUID> sampleTemplateIds,
    String model,
    BigDecimal temperature,
    Integer maxTokens
) {
}
