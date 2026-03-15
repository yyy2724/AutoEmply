package health.autoemplyserver.controller;

import health.autoemplyserver.service.AiModelState;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AiInfoController {

    private final String configuredModel;
    private final AiModelState aiModelState;

    public AiInfoController(@Value("${app.ai.model:claude-sonnet-4-6}") String configuredModel, AiModelState aiModelState) {
        this.configuredModel = configuredModel;
        this.aiModelState = aiModelState;
    }

    @GetMapping("/ai-version")
    public ResponseEntity<?> getAiVersion() {
        String runtimeModel = aiModelState.getLastResponseModel() == null ? configuredModel : aiModelState.getLastResponseModel();
        return ResponseEntity.ok(Map.of(
            "version", runtimeModel,
            "model", runtimeModel,
            "configuredModel", configuredModel,
            "source", aiModelState.getLastResponseModel() == null ? "configured" : "runtime"
        ));
    }
}
