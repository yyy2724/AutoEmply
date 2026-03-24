package health.autoemplyserver.service.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import health.autoemplyserver.dto.prompt.CreatePromptPresetRequest;
import health.autoemplyserver.dto.prompt.PromptPresetDto;
import health.autoemplyserver.entity.ReportTemplate;
import health.autoemplyserver.dto.prompt.UpdatePromptPresetRequest;
import health.autoemplyserver.entity.PromptPreset;
import health.autoemplyserver.entity.PromptVersion;
import health.autoemplyserver.repository.PromptPresetRepository;
import health.autoemplyserver.repository.ReportTemplateRepository;
import health.autoemplyserver.service.sample.SampleTemplateSetService;
import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromptPresetService {

    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.ZERO;
    private static final int DEFAULT_MAX_TOKENS = 32000;
    private static final int MAX_SAMPLE_TEMPLATES = 3;

    private final PromptPresetRepository promptPresetRepository;
    private final ReportTemplateRepository reportTemplateRepository;
    private final SampleTemplateSetService sampleTemplateSetService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.model:" + DEFAULT_MODEL + "}")
    private String configuredModel;

    public PromptPresetService(
        PromptPresetRepository promptPresetRepository,
        ReportTemplateRepository reportTemplateRepository,
        SampleTemplateSetService sampleTemplateSetService,
        ObjectMapper objectMapper
    ) {
        this.promptPresetRepository = promptPresetRepository;
        this.reportTemplateRepository = reportTemplateRepository;
        this.sampleTemplateSetService = sampleTemplateSetService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PromptPresetDto> getAll() {
        return promptPresetRepository.findAllByOrderByActiveDescNameAsc().stream().map(this::toDto).toList();
    }

    public PromptPresetDto create(CreatePromptPresetRequest request) {
        validateRequest(
            request.name(),
            request.systemPrompt(),
            request.userPromptTemplate(),
            request.styleRulesJson(),
            request.sampleTemplateIds()
        );
        OffsetDateTime now = OffsetDateTime.now();

        PromptPreset preset = new PromptPreset();
        preset.setId(UUID.randomUUID());
        preset.setName(request.name().trim());
        preset.setSystemPrompt(request.systemPrompt().trim());
        preset.setUserPromptTemplate(request.userPromptTemplate().trim());
        preset.setStyleRulesJson(normalizeNullable(request.styleRulesJson()));
        preset.setSampleTemplateIdsJson(serializeSampleTemplateIds(request.sampleTemplateIds()));
        preset.setSampleTemplateSetId(null);
        preset.setModel(normalizeNullable(request.model()));
        preset.setTemperature(request.temperature());
        preset.setMaxTokens(request.maxTokens());
        preset.setActive(request.isActive());
        preset.setPrimary(request.isPrimary());
        preset.setCreatedAt(now);
        preset.setUpdatedAt(now);

        promptPresetRepository.save(preset);
        normalizePrimaryPreset(preset);
        saveVersionSnapshot(preset, 1, now);
        return toDto(preset);
    }

    public PromptPresetDto update(UUID id, UpdatePromptPresetRequest request) {
        validateRequest(
            request.name(),
            request.systemPrompt(),
            request.userPromptTemplate(),
            request.styleRulesJson(),
            request.sampleTemplateIds()
        );
        PromptPreset preset = promptPresetRepository.findById(id).orElseThrow(() -> new NotFoundException("Preset not found."));

        preset.setName(request.name().trim());
        preset.setSystemPrompt(request.systemPrompt().trim());
        preset.setUserPromptTemplate(request.userPromptTemplate().trim());
        preset.setStyleRulesJson(normalizeNullable(request.styleRulesJson()));
        preset.setSampleTemplateIdsJson(serializeSampleTemplateIds(request.sampleTemplateIds()));
        preset.setSampleTemplateSetId(null);
        preset.setModel(normalizeNullable(request.model()));
        preset.setTemperature(request.temperature());
        preset.setMaxTokens(request.maxTokens());
        preset.setActive(request.isActive());
        preset.setPrimary(request.isPrimary());
        preset.setUpdatedAt(OffsetDateTime.now());

        normalizePrimaryPreset(preset);
        int nextVersion = preset.getVersions().stream().mapToInt(PromptVersion::getVersion).max().orElse(0) + 1;
        saveVersionSnapshot(preset, nextVersion, OffsetDateTime.now());
        return toDto(preset);
    }

    public void delete(UUID id) {
        if (!promptPresetRepository.existsById(id)) {
            throw new NotFoundException("Preset not found.");
        }
        promptPresetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ResolvedPromptPreset resolve(List<UUID> presetIds, List<UUID> sampleTemplateSetIds) {
        PromptPreset primary;
        List<PromptPreset> references;

        if (presetIds != null && !presetIds.isEmpty()) {
            List<PromptPreset> requestedPresets = loadPresetsInOrder(presetIds);
            primary = requestedPresets.getFirst();
            references = requestedPresets.stream().skip(1).toList();
        } else {
            List<PromptPreset> activePresets = promptPresetRepository.findByActiveTrueOrderByUpdatedAtDesc();
            if (activePresets.isEmpty()) {
                return null;
            }
            List<PromptPreset> orderedActivePresets = orderPresetsForGeneration(activePresets);
            primary = orderedActivePresets.getFirst();
            references = orderedActivePresets.stream().skip(1).toList();
        }

        List<UUID> sampleTemplateIds = resolveSampleTemplateIds(sampleTemplateSetIds);
        return mergePreset(primary, references, sampleTemplateIds);
    }

    private ResolvedPromptPreset mergePreset(PromptPreset primary, List<PromptPreset> references, List<UUID> sampleTemplateIds) {
        return new ResolvedPromptPreset(
            primary.getId(),
            primary.getName(),
            appendSampleTemplateReferences(
                buildPromptWithReferences(primary.getSystemPrompt(), references.stream().map(PromptPreset::getSystemPrompt).toList(), "PastPromptReference"),
                sampleTemplateIds
            ),
            buildPromptWithReferences(primary.getUserPromptTemplate(), references.stream().map(PromptPreset::getUserPromptTemplate).toList(), "PastUserPromptReference"),
            primary.getStyleRulesJson(),
            sampleTemplateIds,
            primary.getModel() == null || primary.getModel().isBlank() ? configuredModel : primary.getModel().trim(),
            primary.getTemperature() == null ? DEFAULT_TEMPERATURE : primary.getTemperature(),
            primary.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : primary.getMaxTokens()
        );
    }

    private List<UUID> resolveSampleTemplateIds(List<UUID> sampleTemplateSetIds) {
        if (sampleTemplateSetIds == null || sampleTemplateSetIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> templateIds = new LinkedHashSet<>();
        for (UUID sampleTemplateSetId : sampleTemplateSetIds) {
            templateIds.addAll(sampleTemplateSetService.parseTemplateIds(sampleTemplateSetService.getEntity(sampleTemplateSetId)));
        }
        return List.copyOf(templateIds);
    }

    private List<PromptPreset> loadPresetsInOrder(List<UUID> presetIds) {
        Map<UUID, PromptPreset> presetsById = promptPresetRepository.findAllById(presetIds).stream()
            .collect(Collectors.toMap(PromptPreset::getId, preset -> preset));
        List<PromptPreset> ordered = new ArrayList<>();
        for (UUID id : presetIds) {
            PromptPreset preset = presetsById.get(id);
            if (preset == null) {
                throw new NotFoundException("Preset not found.");
            }
            ordered.add(preset);
        }
        return ordered;
    }

    private List<PromptPreset> orderPresetsForGeneration(List<PromptPreset> presets) {
        return presets.stream()
            .sorted(Comparator
                .comparing(PromptPreset::isPrimary).reversed()
                .thenComparing(PromptPreset::getUpdatedAt, Comparator.reverseOrder()))
            .toList();
    }

    private String buildPromptWithReferences(String primary, List<String> references, String sectionTitle) {
        List<String> filtered = references.stream()
            .map(reference -> reference == null ? "" : reference.trim())
            .filter(reference -> !reference.isBlank())
            .distinct()
            .toList();
        if (filtered.isEmpty()) {
            return primary == null ? "" : primary.trim();
        }

        StringBuilder builder = new StringBuilder(primary == null ? "" : primary.trim());
        builder.append("\n\n").append(sectionTitle).append(":\n");
        for (int index = 0; index < filtered.size(); index++) {
            builder.append('[').append(index + 1).append("] ").append(filtered.get(index)).append('\n');
        }
        return builder.toString().trim();
    }

    private void saveVersionSnapshot(PromptPreset preset, int version, OffsetDateTime createdAt) {
        PromptVersion promptVersion = new PromptVersion();
        promptVersion.setId(UUID.randomUUID());
        promptVersion.setPreset(preset);
        promptVersion.setVersion(version);
        promptVersion.setSystemPrompt(preset.getSystemPrompt());
        promptVersion.setUserPromptTemplate(preset.getUserPromptTemplate());
        promptVersion.setStyleRulesJson(preset.getStyleRulesJson());
        promptVersion.setCreatedAt(createdAt);
        preset.getVersions().add(promptVersion);
    }

    private void validateRequest(
        String name,
        String systemPrompt,
        String userPromptTemplate,
        String styleRulesJson,
        List<UUID> sampleTemplateIds
    ) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("name is required.");
        }
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new BadRequestException("systemPrompt is required.");
        }
        if (userPromptTemplate == null || userPromptTemplate.isBlank()) {
            throw new BadRequestException("userPromptTemplate is required.");
        }
        if (styleRulesJson != null && !styleRulesJson.isBlank()) {
            try {
                objectMapper.readTree(styleRulesJson);
            } catch (Exception exception) {
                throw new BadRequestException("styleRulesJson is not valid JSON.");
            }
        }
        validateSampleTemplateIds(sampleTemplateIds);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateSampleTemplateIds(List<UUID> sampleTemplateIds) {
        List<UUID> normalized = normalizeSampleTemplateIds(sampleTemplateIds);
        if (normalized.size() > MAX_SAMPLE_TEMPLATES) {
            throw new BadRequestException("sampleTemplateIds can contain up to 3 templates.");
        }
        if (normalized.isEmpty()) {
            return;
        }
        List<UUID> existingIds = reportTemplateRepository.findAllByIdIn(normalized).stream()
            .map(ReportTemplate::getId)
            .toList();
        if (existingIds.size() != normalized.size()) {
            throw new BadRequestException("sampleTemplateIds contains unknown template ids.");
        }
    }

    private String serializeSampleTemplateIds(List<UUID> sampleTemplateIds) {
        List<UUID> normalized = normalizeSampleTemplateIds(sampleTemplateIds);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize sample template ids.", exception);
        }
    }

    private List<UUID> parseSampleTemplateIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            UUID[] values = objectMapper.readValue(json, UUID[].class);
            return normalizeSampleTemplateIds(Arrays.asList(values));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse sample template ids.", exception);
        }
    }

    private List<UUID> normalizeSampleTemplateIds(List<UUID> sampleTemplateIds) {
        if (sampleTemplateIds == null || sampleTemplateIds.isEmpty()) {
            return List.of();
        }
        return sampleTemplateIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .limit(MAX_SAMPLE_TEMPLATES + 1L)
            .toList();
    }

    private String appendSampleTemplateReferences(String prompt, List<UUID> sampleTemplateIds) {
        if (sampleTemplateIds.isEmpty()) {
            return prompt == null ? "" : prompt.trim();
        }

        List<ReportTemplate> templates = loadTemplatesInOrder(sampleTemplateIds);
        if (templates.isEmpty()) {
            return prompt == null ? "" : prompt.trim();
        }

        StringBuilder builder = new StringBuilder(prompt == null ? "" : prompt.trim());
        builder.append("\n\nDelphiSampleReferences:\n")
            .append("Use these uploaded sample files as formatting references for Delphi DFM/PAS structure. ")
            .append("Follow their uses/type/var spacing and declaration comment style when applicable.\n");

        for (int index = 0; index < templates.size(); index++) {
            ReportTemplate template = templates.get(index);
            builder.append('\n')
                .append("[Sample ").append(index + 1).append("]\n")
                .append("Name: ").append(template.getName()).append('\n')
                .append("Category: ").append(template.getCategory()).append('\n')
                .append("OriginalFormName: ").append(template.getOriginalFormName()).append('\n')
                .append("PAS:\n")
                .append(template.getPasContent().trim()).append('\n')
                .append("DFM:\n")
                .append(template.getDfmContent().trim()).append('\n');
        }

        return builder.toString().trim();
    }

    private List<ReportTemplate> loadTemplatesInOrder(List<UUID> sampleTemplateIds) {
        Map<UUID, ReportTemplate> templatesById = reportTemplateRepository.findAllByIdIn(sampleTemplateIds).stream()
            .collect(java.util.stream.Collectors.toMap(ReportTemplate::getId, template -> template));
        List<ReportTemplate> ordered = new ArrayList<>();
        for (UUID id : sampleTemplateIds) {
            ReportTemplate template = templatesById.get(id);
            if (template != null) {
                ordered.add(template);
            }
        }
        return ordered;
    }

    private PromptPresetDto toDto(PromptPreset preset) {
        return new PromptPresetDto(
            preset.getId(),
            preset.getName(),
            preset.getSystemPrompt(),
            preset.getUserPromptTemplate(),
            preset.getStyleRulesJson(),
            parseSampleTemplateIds(preset.getSampleTemplateIdsJson()),
            preset.getModel(),
            preset.getTemperature(),
            preset.getMaxTokens(),
            preset.isActive(),
            preset.isPrimary(),
            preset.getCreatedAt(),
            preset.getUpdatedAt()
        );
    }

    private void normalizePrimaryPreset(PromptPreset target) {
        if (!target.isPrimary()) {
            return;
        }

        promptPresetRepository.findAll().stream()
            .filter(existing -> !existing.getId().equals(target.getId()))
            .filter(PromptPreset::isPrimary)
            .forEach(existing -> existing.setPrimary(false));
    }
}
