package health.autoemplyserver.service.ai;

import health.autoemplyserver.service.prompt.ResolvedPromptPreset;
import java.util.LinkedHashMap;
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", preset.model());
        payload.put("max_tokens", preset.maxTokens());
        if (supportsTemperature(preset.model()) && preset.temperature() != null) {
            payload.put("temperature", preset.temperature());
        }
        payload.put("system", systemPrompt);
        payload.put("tools", List.of(toolSchema));
        payload.put("tool_choice", Map.of("type", "tool", "name", toolName));
        payload.put("messages", List.of(
            Map.of(
                "role", "user",
                "content", List.of(
                    Map.of("type", "text", "text", userPrompt),
                    buildVisualBlock(mediaType, base64Data)
                )
            )
        ));
        return payload;
    }

    private boolean supportsTemperature(String model) {
        if (model == null) {
            return true;
        }
        String normalized = model.trim().toLowerCase();
        return !normalized.startsWith("claude-opus-4-7");
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
