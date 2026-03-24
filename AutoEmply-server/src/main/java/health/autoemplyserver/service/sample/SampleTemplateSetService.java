package health.autoemplyserver.service.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import health.autoemplyserver.dto.sample.CreateSampleTemplateSetRequest;
import health.autoemplyserver.dto.sample.SampleTemplateSetDto;
import health.autoemplyserver.dto.sample.UpdateSampleTemplateSetRequest;
import health.autoemplyserver.entity.ReportTemplate;
import health.autoemplyserver.entity.SampleTemplateSet;
import health.autoemplyserver.repository.ReportTemplateRepository;
import health.autoemplyserver.repository.SampleTemplateSetRepository;
import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SampleTemplateSetService {

    private final SampleTemplateSetRepository sampleTemplateSetRepository;
    private final ReportTemplateRepository reportTemplateRepository;
    private final ObjectMapper objectMapper;

    public SampleTemplateSetService(
        SampleTemplateSetRepository sampleTemplateSetRepository,
        ReportTemplateRepository reportTemplateRepository,
        ObjectMapper objectMapper
    ) {
        this.sampleTemplateSetRepository = sampleTemplateSetRepository;
        this.reportTemplateRepository = reportTemplateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SampleTemplateSetDto> getAll() {
        return sampleTemplateSetRepository.findAll().stream()
            .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public SampleTemplateSet getEntity(UUID id) {
        return sampleTemplateSetRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Sample template set not found."));
    }

    public SampleTemplateSetDto create(CreateSampleTemplateSetRequest request) {
        List<UUID> templateIds = validateTemplateIds(request.name(), request.templateIds());
        OffsetDateTime now = OffsetDateTime.now();

        SampleTemplateSet sampleTemplateSet = SampleTemplateSet.builder()
            .id(UUID.randomUUID())
            .name(request.name().trim())
            .templateIdsJson(serializeTemplateIds(templateIds))
            .active(request.isActive())
            .primary(request.isPrimary())
            .createdAt(now)
            .updatedAt(now)
            .build();

        sampleTemplateSetRepository.save(sampleTemplateSet);
        normalizePrimarySampleSet(sampleTemplateSet);
        return toDto(sampleTemplateSet);
    }

    public SampleTemplateSetDto update(UUID id, UpdateSampleTemplateSetRequest request) {
        List<UUID> templateIds = validateTemplateIds(request.name(), request.templateIds());
        SampleTemplateSet sampleTemplateSet = getEntity(id);
        sampleTemplateSet.setName(request.name().trim());
        sampleTemplateSet.setTemplateIdsJson(serializeTemplateIds(templateIds));
        sampleTemplateSet.setActive(request.isActive());
        sampleTemplateSet.setPrimary(request.isPrimary());
        sampleTemplateSet.setUpdatedAt(OffsetDateTime.now());
        normalizePrimarySampleSet(sampleTemplateSet);
        return toDto(sampleTemplateSet);
    }

    public void delete(UUID id) {
        if (!sampleTemplateSetRepository.existsById(id)) {
            throw new NotFoundException("Sample template set not found.");
        }
        sampleTemplateSetRepository.deleteById(id);
    }

    public List<UUID> parseTemplateIds(SampleTemplateSet sampleTemplateSet) {
        return parseTemplateIds(sampleTemplateSet.getTemplateIdsJson());
    }

    private List<UUID> validateTemplateIds(String name, List<UUID> templateIds) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("name is required.");
        }
        List<UUID> normalized = normalizeTemplateIds(templateIds);
        if (normalized.isEmpty()) {
            throw new BadRequestException("templateIds must contain at least one template id.");
        }
        List<UUID> existingIds = reportTemplateRepository.findAllByIdIn(normalized).stream()
            .map(ReportTemplate::getId)
            .toList();
        if (existingIds.size() != normalized.size()) {
            throw new BadRequestException("templateIds contains unknown template ids.");
        }
        return normalized;
    }

    private String serializeTemplateIds(List<UUID> templateIds) {
        try {
            return objectMapper.writeValueAsString(templateIds);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize template ids.", exception);
        }
    }

    private List<UUID> parseTemplateIds(String templateIdsJson) {
        if (templateIdsJson == null || templateIdsJson.isBlank()) {
            return List.of();
        }
        try {
            UUID[] values = objectMapper.readValue(templateIdsJson, UUID[].class);
            return normalizeTemplateIds(Arrays.asList(values));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse template ids.", exception);
        }
    }

    private List<UUID> normalizeTemplateIds(List<UUID> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return List.of();
        }
        return templateIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private SampleTemplateSetDto toDto(SampleTemplateSet sampleTemplateSet) {
        return new SampleTemplateSetDto(
            sampleTemplateSet.getId(),
            sampleTemplateSet.getName(),
            parseTemplateIds(sampleTemplateSet),
            sampleTemplateSet.isActive(),
            sampleTemplateSet.isPrimary(),
            sampleTemplateSet.getCreatedAt(),
            sampleTemplateSet.getUpdatedAt()
        );
    }

    public List<SampleTemplateSet> getActiveSetsOrderedForGeneration() {
        return sampleTemplateSetRepository.findAll().stream()
            .filter(SampleTemplateSet::isActive)
            .sorted(Comparator
                .comparing(SampleTemplateSet::isPrimary).reversed()
                .thenComparing(SampleTemplateSet::getUpdatedAt, Comparator.reverseOrder()))
            .toList();
    }

    private void normalizePrimarySampleSet(SampleTemplateSet target) {
        if (!target.isPrimary()) {
            return;
        }

        sampleTemplateSetRepository.findAll().stream()
            .filter(existing -> !existing.getId().equals(target.getId()))
            .filter(SampleTemplateSet::isPrimary)
            .forEach(existing -> existing.setPrimary(false));
    }
}
