package health.autoemplyserver.service;

import health.autoemplyserver.model.FormStructure;
import health.autoemplyserver.model.LayoutSpec;
import health.autoemplyserver.service.image.PreparedVisual;
import health.autoemplyserver.service.image.UploadedVisualPreparer;
import health.autoemplyserver.service.prompt.PromptPresetResolver;
import health.autoemplyserver.service.prompt.ResolvedPromptPreset;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageGenerationService {

    private final ClaudeClient claudeClient;
    private final DelphiGenerator delphiGenerator;
    private final StructureToLayoutConverter structureToLayoutConverter;
    private final LayoutPostProcessor layoutPostProcessor;
    private final UploadedVisualPreparer uploadedVisualPreparer;
    private final PromptPresetResolver promptPresetResolver;

    public ImageGenerationService(
        ClaudeClient claudeClient,
        DelphiGenerator delphiGenerator,
        StructureToLayoutConverter structureToLayoutConverter,
        LayoutPostProcessor layoutPostProcessor,
        UploadedVisualPreparer uploadedVisualPreparer,
        PromptPresetResolver promptPresetResolver
    ) {
        this.claudeClient = claudeClient;
        this.delphiGenerator = delphiGenerator;
        this.structureToLayoutConverter = structureToLayoutConverter;
        this.layoutPostProcessor = layoutPostProcessor;
        this.uploadedVisualPreparer = uploadedVisualPreparer;
        this.promptPresetResolver = promptPresetResolver;
    }

    public LayoutSpec generateLayoutSpec(String formName, MultipartFile image, String presetId, String sampleTemplateSetId) {
        PreparedVisual visual = uploadedVisualPreparer.prepare(formName, image);
        ResolvedPromptPreset preset = promptPresetResolver.resolve(presetId, sampleTemplateSetId);
        LayoutSpec layoutSpec = claudeClient.generateLayoutSpec(visual.formName(), visual.mediaType(), visual.base64Data(), preset);
        return layoutPostProcessor.process(layoutSpec);
    }

    public byte[] exportZip(String formName, MultipartFile image, String presetId, String sampleTemplateSetId) {
        LayoutSpec layoutSpec = generateLayoutSpec(formName, image, presetId, sampleTemplateSetId);
        return delphiGenerator.generateZip(formName.trim(), layoutSpec);
    }

    public byte[] exportZipFromStructure(String formName, MultipartFile image, String presetId, String sampleTemplateSetId) {
        LayoutSpec layoutSpec = generateLayoutSpecFromStructure(formName, image, presetId, sampleTemplateSetId);
        return delphiGenerator.generateZip(formName.trim(), layoutSpec);
    }

    public FormStructure generateStructure(String formName, MultipartFile image, String presetId, String sampleTemplateSetId) {
        PreparedVisual visual = uploadedVisualPreparer.prepare(formName, image);
        ResolvedPromptPreset preset = promptPresetResolver.resolve(presetId, sampleTemplateSetId);
        return claudeClient.generateFormStructure(visual.formName(), visual.mediaType(), visual.base64Data(), preset);
    }

    public LayoutSpec generateLayoutSpecFromStructure(String formName, MultipartFile image, String presetId, String sampleTemplateSetId) {
        FormStructure structure = generateStructure(formName, image, presetId, sampleTemplateSetId);
        return layoutPostProcessor.process(structureToLayoutConverter.convert(structure));
    }
}
