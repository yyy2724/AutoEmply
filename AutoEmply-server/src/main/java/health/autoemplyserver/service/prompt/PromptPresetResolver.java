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

    public ResolvedPromptPreset resolve(String presetId) {
        UUID id = parsePresetId(presetId);
        ResolvedPromptPreset preset = promptPresetService.resolve(id);
        if (preset == null) {
            throw new NotFoundException("Prompt preset not found.");
        }
        return preset;
    }

    private UUID parsePresetId(String presetId) {
        if (presetId == null || presetId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(presetId.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("presetId is not a valid UUID.");
        }
    }
}
