package health.autoemplyserver.service;

import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.image.PreparedVisual;
import health.autoemplyserver.service.image.UploadedVisualPreparer;
import health.autoemplyserver.service.prompt.PromptPresetResolver;
import health.autoemplyserver.service.prompt.ResolvedPromptPreset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageGenerationService {

    private final ClaudeClient claudeClient;
    private final DelphiGenerator delphiGenerator;
    private final StructureToLayoutConverter structureToLayoutConverter;
    private final UploadedVisualPreparer uploadedVisualPreparer;
    private final PromptPresetResolver promptPresetResolver;

    public ImageGenerationService(
        ClaudeClient claudeClient,
        DelphiGenerator delphiGenerator,
        StructureToLayoutConverter structureToLayoutConverter,
        UploadedVisualPreparer uploadedVisualPreparer,
        PromptPresetResolver promptPresetResolver
    ) {
        this.claudeClient = claudeClient;
        this.delphiGenerator = delphiGenerator;
        this.structureToLayoutConverter = structureToLayoutConverter;
        this.uploadedVisualPreparer = uploadedVisualPreparer;
        this.promptPresetResolver = promptPresetResolver;
    }

    public LayoutSpec generateLayoutSpec(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        PreparedVisual visual = uploadedVisualPreparer.prepare(formName, image);
        ResolvedPromptPreset preset = promptPresetResolver.resolve(presetIds, sampleTemplateSetIds);
        LayoutSpec layoutSpec = claudeClient.generateLayoutSpec(visual.formName(), visual.mediaType(), visual.base64Data(), preset);
        return layoutSpec;
    }

    public byte[] exportZip(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        LayoutSpec layoutSpec = generateLayoutSpec(formName, image, presetIds, sampleTemplateSetIds);
        return delphiGenerator.generateZip(formName.trim(), layoutSpec);
    }

    public byte[] exportZipFromStructure(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        LayoutSpec layoutSpec = generateLayoutSpecFromStructure(formName, image, presetIds, sampleTemplateSetIds);
        return delphiGenerator.generateZip(formName.trim(), layoutSpec);
    }

    public FormStructure generateStructure(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        PreparedVisual visual = uploadedVisualPreparer.prepare(formName, image);
        ResolvedPromptPreset preset = promptPresetResolver.resolve(presetIds, sampleTemplateSetIds);
        return claudeClient.generateFormStructure(visual.formName(), visual.mediaType(), visual.base64Data(), preset);
    }

    public LayoutSpec generateLayoutSpecFromStructure(String formName, MultipartFile image, List<String> presetIds, List<String> sampleTemplateSetIds) {
        FormStructure structure = generateStructure(formName, image, presetIds, sampleTemplateSetIds);
        return structureToLayoutConverter.convert(structure);
    }
}
