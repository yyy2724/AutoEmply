package health.autoemplyserver.application.image;

import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.ImageGenerationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageGenerationApplicationService {

    private final ImageGenerationService imageGenerationService;

    public ImageGenerationApplicationService(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    public LayoutSpec generateLayout(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        return imageGenerationService.generateLayoutSpec(formName, image, presetIds, sampleTemplateSetIds);
    }

    public LayoutSpec generateLayoutFromStructure(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        return imageGenerationService.generateLayoutSpecFromStructure(formName, image, presetIds, sampleTemplateSetIds);
    }

    public FormStructure generateStructure(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        return imageGenerationService.generateStructure(formName, image, presetIds, sampleTemplateSetIds);
    }

    public byte[] exportZip(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        return imageGenerationService.exportZip(formName, image, presetIds, sampleTemplateSetIds);
    }

    public byte[] exportZipFromStructure(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        return imageGenerationService.exportZipFromStructure(formName, image, presetIds, sampleTemplateSetIds);
    }
}
