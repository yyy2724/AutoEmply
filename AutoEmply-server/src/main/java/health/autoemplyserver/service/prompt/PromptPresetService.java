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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single responsibility: CRUD persistence of prompt presets — create/update/delete with
 * validation, primary-flag normalization and version snapshots, plus listing as DTOs.
 * Prompt composition/resolution into a {@link ResolvedPromptPreset} for generation calls
 * lives in {@link PromptResolver}.
 */
@Service
@Transactional
public class PromptPresetService {

    private final PromptPresetRepository promptPresetRepository;
    private final ObjectMapper objectMapper;

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
        validateRequest(
            request.name(),
            request.systemPrompt(),
            request.userPromptTemplate(),
            request.styleRulesJson()
        );
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
            request.styleRulesJson()
        );
        PromptPreset preset = promptPresetRepository.findById(id).orElseThrow(() -> new NotFoundException("Preset not found."));

        preset.setName(request.name().trim());
        preset.setSystemPrompt(request.systemPrompt().trim());
        preset.setUserPromptTemplate(request.userPromptTemplate().trim());
        preset.setStyleRulesJson(normalizeNullable(request.styleRulesJson()));
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
        String styleRulesJson
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
