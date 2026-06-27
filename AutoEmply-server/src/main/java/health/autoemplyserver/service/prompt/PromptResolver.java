package health.autoemplyserver.service.prompt;

import health.autoemplyserver.entity.PromptPreset;
import health.autoemplyserver.repository.PromptPresetRepository;
import health.autoemplyserver.service.AiModelSelectionService;
import health.autoemplyserver.support.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single responsibility: prompt composition/resolution. Selects the primary preset and its
 * references (either from an explicit ordered id list or from the active presets), merges
 * them into one {@link ResolvedPromptPreset} for Claude generation calls, and applies the
 * merge-time fallbacks (temperature, max tokens, selected model). Persistence/CRUD of
 * presets lives in {@link PromptPresetService}.
 */
@Service
public class PromptResolver {

    /**
     * Historical default model name. Currently NOT consulted anywhere: at resolve/merge
     * time the model always comes from {@link AiModelSelectionService#getSelectedModel()}
     * (which applies its own fallback), and a preset's stored {@code model} field is
     * ignored for generation.
     */
    private static final String DEFAULT_MODEL = "claude-opus-4-7";

    /**
     * Fallback temperature applied at resolve/merge time only (see {@code mergePreset}):
     * used when the primary preset's stored temperature is null. Creation/update persist
     * the request value as-is, including null.
     */
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.ZERO;

    /**
     * Fallback max-tokens applied at resolve/merge time only (see {@code mergePreset}):
     * used when the primary preset's stored maxTokens is null. Creation/update persist
     * the request value as-is, including null.
     */
    private static final int DEFAULT_MAX_TOKENS = 32000;

    private final PromptPresetRepository promptPresetRepository;
    private final AiModelSelectionService aiModelSelectionService;

    public PromptResolver(
        PromptPresetRepository promptPresetRepository,
        AiModelSelectionService aiModelSelectionService
    ) {
        this.promptPresetRepository = promptPresetRepository;
        this.aiModelSelectionService = aiModelSelectionService;
    }

    @Transactional(readOnly = true)
    public ResolvedPromptPreset resolve(List<UUID> presetIds) {
        List<PromptPreset> orderedPresets;
        if (presetIds != null && !presetIds.isEmpty()) {
            orderedPresets = loadPresetsInOrder(presetIds);
        } else {
            List<PromptPreset> activePresets = promptPresetRepository.findByActiveTrueOrderByUpdatedAtDesc();
            if (activePresets.isEmpty()) {
                return null;
            }
            orderedPresets = orderPresetsForGeneration(activePresets);
        }

        return mergePreset(orderedPresets.getFirst(), orderedPresets.stream().skip(1).toList());
    }

    private ResolvedPromptPreset mergePreset(PromptPreset primary, List<PromptPreset> references) {
        return new ResolvedPromptPreset(
            primary.getId(),
            primary.getName(),
            buildPromptWithReferences(primary.getSystemPrompt(), references.stream().map(PromptPreset::getSystemPrompt).toList(), "PastPromptReference"),
            buildPromptWithReferences(primary.getUserPromptTemplate(), references.stream().map(PromptPreset::getUserPromptTemplate).toList(), "PastUserPromptReference"),
            primary.getStyleRulesJson(),
            aiModelSelectionService.getSelectedModel(),
            primary.getTemperature() == null ? DEFAULT_TEMPERATURE : primary.getTemperature(),
            primary.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : primary.getMaxTokens()
        );
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
}
