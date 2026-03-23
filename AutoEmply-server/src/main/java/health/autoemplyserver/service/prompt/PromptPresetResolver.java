package health.autoemplyserver.service.prompt;

import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PromptPresetResolver {

    private final PromptPresetService promptPresetService;

    public PromptPresetResolver(PromptPresetService promptPresetService) {
        this.promptPresetService = promptPresetService;
    }

    public ResolvedPromptPreset resolve(String presetId, String sampleTemplateSetId) {
        UUID parsedPresetId = parseUuid(presetId, "presetId");
        UUID parsedSetId = parseUuid(sampleTemplateSetId, "sampleTemplateSetId");
        ResolvedPromptPreset preset = promptPresetService.resolve(parsedPresetId, parsedSetId);
        if (preset == null) {
            throw new NotFoundException("Prompt preset not found.");
        }
        return preset;
    }

    private UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(fieldName + " is not a valid UUID.");
        }
    }
}
