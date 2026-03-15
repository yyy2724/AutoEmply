package health.autoemplyserver.application.image;

import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.ImageGenerationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageGenerationApplicationService {

    private final ImageGenerationService imageGenerationService;

    public ImageGenerationApplicationService(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    public LayoutSpec generateLayout(String formName, MultipartFile image, String presetId) {
        return imageGenerationService.generateLayoutSpec(formName, image, presetId);
    }

    public LayoutSpec generateLayoutFromStructure(String formName, MultipartFile image, String presetId) {
        return imageGenerationService.generateLayoutSpecFromStructure(formName, image, presetId);
    }

    public FormStructure generateStructure(String formName, MultipartFile image, String presetId) {
        return imageGenerationService.generateStructure(formName, image, presetId);
    }

    public byte[] exportZip(String formName, MultipartFile image, String presetId) {
        return imageGenerationService.exportZip(formName, image, presetId);
    }

    public byte[] exportZipFromStructure(String formName, MultipartFile image, String presetId) {
        return imageGenerationService.exportZipFromStructure(formName, image, presetId);
    }
}
