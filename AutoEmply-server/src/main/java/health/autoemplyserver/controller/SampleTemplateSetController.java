package health.autoemplyserver.controller;

import health.autoemplyserver.dto.sample.CreateSampleTemplateSetRequest;
import health.autoemplyserver.dto.sample.UpdateSampleTemplateSetRequest;
import health.autoemplyserver.service.sample.SampleTemplateSetService;
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
@RequestMapping("/api/sample-template-sets")
public class SampleTemplateSetController {

    private final SampleTemplateSetService sampleTemplateSetService;

    public SampleTemplateSetController(SampleTemplateSetService sampleTemplateSetService) {
        this.sampleTemplateSetService = sampleTemplateSetService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(sampleTemplateSetService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateSampleTemplateSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sampleTemplateSetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateSampleTemplateSetRequest request) {
        return ResponseEntity.ok(sampleTemplateSetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        sampleTemplateSetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
