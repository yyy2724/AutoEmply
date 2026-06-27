package health.autoemplyserver.service.prompt;

import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PromptPresetResolver {

    private final PromptResolver promptResolver;

    public PromptPresetResolver(PromptResolver promptResolver) {
        this.promptResolver = promptResolver;
    }

    public ResolvedPromptPreset resolve(List<String> presetIds) {
        List<UUID> parsedPresetIds = parseUuids(presetIds, "presetIds");
        ResolvedPromptPreset preset = promptResolver.resolve(parsedPresetIds);
        if (preset == null) {
            throw new NotFoundException("Prompt preset not found.");
        }
        return preset;
    }

    private List<UUID> parseUuids(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> parsedValues = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                parsedValues.add(UUID.fromString(value.trim()));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException(fieldName + " contains an invalid UUID.");
            }
        }
        return List.copyOf(parsedValues);
    }
}
