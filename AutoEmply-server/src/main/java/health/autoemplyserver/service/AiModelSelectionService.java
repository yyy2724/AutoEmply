package health.autoemplyserver.service;

import health.autoemplyserver.config.AiProperties;
import health.autoemplyserver.support.exception.BadRequestException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AiModelSelectionService {

    private static final Set<String> ALLOWED_MODELS = Set.of(
        "claude-sonnet-4-6",
        "claude-opus-4-6"
    );

    private final AiProperties aiProperties;
    private volatile String selectedModel;

    public AiModelSelectionService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.selectedModel = normalize(aiProperties.getModel());
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public String updateSelectedModel(String model) {
        String normalized = normalize(model);
        if (!ALLOWED_MODELS.contains(normalized)) {
            throw new BadRequestException("Unsupported AI model.", ALLOWED_MODELS.stream().sorted().toList());
        }
        selectedModel = normalized;
        aiProperties.setModel(normalized);
        return selectedModel;
    }

    private String normalize(String model) {
        String normalized = model == null || model.isBlank() ? "claude-sonnet-4-6" : model.trim();
        if (!ALLOWED_MODELS.contains(normalized)) {
            return "claude-sonnet-4-6";
        }
        return normalized;
    }
}
