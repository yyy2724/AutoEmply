package health.autoemplyserver.controller;

import health.autoemplyserver.dto.ai.UpdateAiModelRequest;
import health.autoemplyserver.service.AiModelSelectionService;
import health.autoemplyserver.service.AiModelState;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AiInfoController {

    private final AiModelSelectionService aiModelSelectionService;
    private final AiModelState aiModelState;

    public AiInfoController(AiModelSelectionService aiModelSelectionService, AiModelState aiModelState) {
        this.aiModelSelectionService = aiModelSelectionService;
        this.aiModelState = aiModelState;
    }

    @GetMapping("/ai-version")
    public ResponseEntity<?> getAiVersion() {
        String configuredModel = aiModelSelectionService.getSelectedModel();
        String runtimeModel = aiModelState.getLastResponseModel() == null ? configuredModel : aiModelState.getLastResponseModel();
        return ResponseEntity.ok(Map.of(
            "version", runtimeModel,
            "model", runtimeModel,
            "configuredModel", configuredModel,
            "source", aiModelState.getLastResponseModel() == null ? "configured" : "runtime"
        ));
    }

    @PutMapping("/ai-version")
    public ResponseEntity<?> updateAiVersion(@RequestBody UpdateAiModelRequest request) {
        String selectedModel = aiModelSelectionService.updateSelectedModel(request.model());
        return ResponseEntity.ok(Map.of(
            "version", selectedModel,
            "model", selectedModel,
            "configuredModel", selectedModel,
            "source", "configured"
        ));
    }
}
