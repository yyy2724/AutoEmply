package health.autoemplyserver.service.ai;

import health.autoemplyserver.service.prompt.ResolvedPromptPreset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClaudePayloadFactory {

    public Map<String, Object> build(
        ResolvedPromptPreset preset,
        String systemPrompt,
        Map<String, Object> toolSchema,
        String toolName,
        String userPrompt,
        String mediaType,
        String base64Data
    ) {
        return Map.of(
            "model", preset.model(),
            "max_tokens", preset.maxTokens(),
            "temperature", preset.temperature(),
            "system", systemPrompt,
            "tools", List.of(toolSchema),
            "tool_choice", Map.of("type", "tool", "name", toolName),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", List.of(
                        Map.of("type", "text", "text", userPrompt),
                        buildVisualBlock(mediaType, base64Data)
                    )
                )
            )
        );
    }

    public String renderUserPrompt(String template, String formName) {
        return (template == null ? "" : template)
            .replace("{{formName}}", formName)
            .replace("{formName}", formName)
            .trim();
    }

    private Map<String, Object> buildVisualBlock(String mediaType, String base64Data) {
        return Map.of(
            "type", "application/pdf".equalsIgnoreCase(mediaType) ? "document" : "image",
            "source", Map.of(
                "type", "base64",
                "media_type", mediaType,
                "data", base64Data
            )
        );
    }
}
