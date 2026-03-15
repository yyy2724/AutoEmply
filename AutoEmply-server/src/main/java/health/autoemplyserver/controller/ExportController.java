package health.autoemplyserver.controller;

import health.autoemplyserver.application.export.LayoutExportApplicationService;
import health.autoemplyserver.dto.export.ExportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final LayoutExportApplicationService layoutExportApplicationService;

    public ExportController(LayoutExportApplicationService layoutExportApplicationService) {
        this.layoutExportApplicationService = layoutExportApplicationService;
    }

    @PostMapping
    public ResponseEntity<?> export(@Valid @RequestBody ExportRequest request) {
        String formName = request.formName().trim();
        byte[] payload = layoutExportApplicationService.export(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + formName + ".zip\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(payload);
    }
}
