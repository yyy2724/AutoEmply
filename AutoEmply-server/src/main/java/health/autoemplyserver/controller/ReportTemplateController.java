package health.autoemplyserver.controller;

import health.autoemplyserver.application.report.ReportTemplateApplicationService;
import health.autoemplyserver.entity.ReportTemplate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/report-templates")
public class ReportTemplateController {

    private final ReportTemplateApplicationService reportTemplateApplicationService;

    public ReportTemplateController(ReportTemplateApplicationService reportTemplateApplicationService) {
        this.reportTemplateApplicationService = reportTemplateApplicationService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(reportTemplateApplicationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return ResponseEntity.ok(reportTemplateApplicationService.get(id));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<?> preview(@PathVariable UUID id) {
        ReportTemplate template = reportTemplateApplicationService.getPreviewSource(id);
        MediaType mediaType = template.getPreviewContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(template.getPreviewContentType());
        return ResponseEntity.ok().contentType(mediaType).body(template.getPreviewData());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
        @RequestParam String name,
        @RequestParam String category,
        @RequestParam MultipartFile dfmFile,
        @RequestParam MultipartFile pasFile,
        @RequestParam(required = false) MultipartFile previewFile
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportTemplateApplicationService.create(name, category, dfmFile, pasFile, previewFile));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable UUID id, @RequestParam String formName) {
        byte[] payload = reportTemplateApplicationService.download(id, formName);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + formName.trim() + ".zip\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(payload);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        reportTemplateApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
