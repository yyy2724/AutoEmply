package health.autoemplyserver.controller;

import health.autoemplyserver.application.image.ImageGenerationApplicationService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImageExportController {

    private final ImageGenerationApplicationService imageGenerationApplicationService;

    public ImageExportController(ImageGenerationApplicationService imageGenerationApplicationService) {
        this.imageGenerationApplicationService = imageGenerationApplicationService;
    }

    @PostMapping(path = "/generate-json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateJson(@RequestParam String formName, @RequestParam MultipartFile image, @RequestParam(required = false) List<String> presetIds) {
        return ResponseEntity.ok(imageGenerationApplicationService.generateLayout(formName, image, presetIds));
    }

    @PostMapping(path = "/export-from-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> exportFromImage(@RequestParam String formName, @RequestParam MultipartFile image, @RequestParam(required = false) List<String> presetIds) {
        byte[] payload = imageGenerationApplicationService.exportZip(formName, image, presetIds);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + formName.trim() + ".zip\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(payload);
    }

    @PostMapping(path = "/documents/{docId}/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateDocumentLayout(@PathVariable String docId, @RequestParam String formName, @RequestParam MultipartFile image, @RequestParam(required = false) List<String> presetIds) {
        return ResponseEntity.ok(imageGenerationApplicationService.generateLayout(formName, image, presetIds));
    }

    @PostMapping(path = "/generate-json-v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateJsonV2(@RequestParam String formName, @RequestParam MultipartFile image, @RequestParam(required = false) List<String> presetIds) {
        return ResponseEntity.ok(imageGenerationApplicationService.generateLayoutFromStructure(formName, image, presetIds));
    }

    @PostMapping(path = "/export-from-image-v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> exportFromImageV2(@RequestParam String formName, @RequestParam MultipartFile image, @RequestParam(required = false) List<String> presetIds) {
        byte[] payload = imageGenerationApplicationService.exportZipFromStructure(formName, image, presetIds);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + formName.trim() + ".zip\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(payload);
    }

    @PostMapping(path = "/generate-structure", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateStructure(@RequestParam String formName, @RequestParam MultipartFile image, @RequestParam(required = false) List<String> presetIds) {
        return ResponseEntity.ok(imageGenerationApplicationService.generateStructure(formName, image, presetIds));
    }
}
