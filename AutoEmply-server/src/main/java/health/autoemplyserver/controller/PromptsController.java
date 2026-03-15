package health.autoemplyserver.controller;

import health.autoemplyserver.application.prompt.PromptPresetApplicationService;
import health.autoemplyserver.dto.prompt.CreatePromptPresetRequest;
import health.autoemplyserver.dto.prompt.UpdatePromptPresetRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompts")
public class PromptsController {

    private final PromptPresetApplicationService promptPresetApplicationService;

    public PromptsController(PromptPresetApplicationService promptPresetApplicationService) {
        this.promptPresetApplicationService = promptPresetApplicationService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(promptPresetApplicationService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreatePromptPresetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promptPresetApplicationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdatePromptPresetRequest request) {
        return ResponseEntity.ok(promptPresetApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        promptPresetApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
