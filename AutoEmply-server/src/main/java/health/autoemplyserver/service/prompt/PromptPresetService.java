package health.autoemplyserver.service.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import health.autoemplyserver.dto.prompt.CreatePromptPresetRequest;
import health.autoemplyserver.dto.prompt.PromptPresetDto;
import health.autoemplyserver.dto.prompt.UpdatePromptPresetRequest;
import health.autoemplyserver.entity.PromptPreset;
import health.autoemplyserver.entity.PromptVersion;
import health.autoemplyserver.repository.PromptPresetRepository;
import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PromptPresetService {

    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.ZERO;
    private static final int DEFAULT_MAX_TOKENS = 32000;

    private final PromptPresetRepository promptPresetRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.model:" + DEFAULT_MODEL + "}")
    private String configuredModel;

    public PromptPresetService(
        PromptPresetRepository promptPresetRepository,
        ObjectMapper objectMapper
    ) {
        this.promptPresetRepository = promptPresetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PromptPresetDto> getAll() {
        return promptPresetRepository.findAllByOrderByActiveDescNameAsc().stream().map(this::toDto).toList();
    }

    public PromptPresetDto create(CreatePromptPresetRequest request) {
        validateRequest(request.name(), request.systemPrompt(), request.userPromptTemplate(), request.styleRulesJson());
        OffsetDateTime now = OffsetDateTime.now();

        PromptPreset preset = new PromptPreset();
        preset.setId(UUID.randomUUID());
        preset.setName(request.name().trim());
        preset.setSystemPrompt(request.systemPrompt().trim());
        preset.setUserPromptTemplate(request.userPromptTemplate().trim());
        preset.setStyleRulesJson(normalizeNullable(request.styleRulesJson()));
        preset.setModel(normalizeNullable(request.model()));
        preset.setTemperature(request.temperature());
        preset.setMaxTokens(request.maxTokens());
        preset.setActive(request.isActive());
        preset.setCreatedAt(now);
        preset.setUpdatedAt(now);

        promptPresetRepository.save(preset);
        saveVersionSnapshot(preset, 1, now);
        return toDto(preset);
    }

    public PromptPresetDto update(UUID id, UpdatePromptPresetRequest request) {
        validateRequest(request.name(), request.systemPrompt(), request.userPromptTemplate(), request.styleRulesJson());
        PromptPreset preset = promptPresetRepository.findById(id).orElseThrow(() -> new NotFoundException("Preset not found."));

        preset.setName(request.name().trim());
        preset.setSystemPrompt(request.systemPrompt().trim());
        preset.setUserPromptTemplate(request.userPromptTemplate().trim());
        preset.setStyleRulesJson(normalizeNullable(request.styleRulesJson()));
        preset.setModel(normalizeNullable(request.model()));
        preset.setTemperature(request.temperature());
        preset.setMaxTokens(request.maxTokens());
        preset.setActive(request.isActive());
        preset.setUpdatedAt(OffsetDateTime.now());

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
    public ResolvedPromptPreset resolve(UUID presetId) {
        if (presetId != null) {
            PromptPreset preset = promptPresetRepository.findById(presetId).orElse(null);
            if (preset == null) {
                return null;
            }
            return mergePreset(preset, promptPresetRepository.findByActiveTrueAndIdNotOrderByUpdatedAtDesc(preset.getId()));
        }

        List<PromptPreset> activePresets = promptPresetRepository.findByActiveTrueOrderByUpdatedAtDesc();
        if (activePresets.isEmpty()) {
            return null;
        }
        return mergePreset(activePresets.getFirst(), activePresets.stream().skip(1).toList());
    }

    private ResolvedPromptPreset mergePreset(PromptPreset primary, List<PromptPreset> references) {
        return new ResolvedPromptPreset(
            primary.getId(),
            primary.getName(),
            buildPromptWithReferences(primary.getSystemPrompt(), references.stream().map(PromptPreset::getSystemPrompt).toList(), "PastPromptReference"),
            buildPromptWithReferences(primary.getUserPromptTemplate(), references.stream().map(PromptPreset::getUserPromptTemplate).toList(), "PastUserPromptReference"),
            primary.getStyleRulesJson(),
            primary.getModel() == null || primary.getModel().isBlank() ? configuredModel : primary.getModel().trim(),
            primary.getTemperature() == null ? DEFAULT_TEMPERATURE : primary.getTemperature(),
            primary.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : primary.getMaxTokens()
        );
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

    private void validateRequest(String name, String systemPrompt, String userPromptTemplate, String styleRulesJson) {
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
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PromptPresetDto toDto(PromptPreset preset) {
        return new PromptPresetDto(
            preset.getId(),
            preset.getName(),
            preset.getSystemPrompt(),
            preset.getUserPromptTemplate(),
            preset.getStyleRulesJson(),
            preset.getModel(),
            preset.getTemperature(),
            preset.getMaxTokens(),
            preset.isActive(),
            preset.getCreatedAt(),
            preset.getUpdatedAt()
        );
    }
}
