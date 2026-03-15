package health.autoemplyserver.application.prompt;

import health.autoemplyserver.dto.prompt.CreatePromptPresetRequest;
import health.autoemplyserver.dto.prompt.PromptPresetDto;
import health.autoemplyserver.dto.prompt.UpdatePromptPresetRequest;
import health.autoemplyserver.service.prompt.PromptPresetService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PromptPresetApplicationService {

    private final PromptPresetService promptPresetService;

    public PromptPresetApplicationService(PromptPresetService promptPresetService) {
        this.promptPresetService = promptPresetService;
    }

    public List<PromptPresetDto> getAll() {
        return promptPresetService.getAll();
    }

    public PromptPresetDto create(CreatePromptPresetRequest request) {
        return promptPresetService.create(request);
    }

    public PromptPresetDto update(UUID id, UpdatePromptPresetRequest request) {
        return promptPresetService.update(id, request);
    }

    public void delete(UUID id) {
        promptPresetService.delete(id);
    }
}
