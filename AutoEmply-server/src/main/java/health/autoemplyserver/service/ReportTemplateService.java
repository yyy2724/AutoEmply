package health.autoemplyserver.service;

import health.autoemplyserver.dto.report.ReportTemplateCreateResponse;
import health.autoemplyserver.dto.report.ReportTemplateDetailDto;
import health.autoemplyserver.dto.report.ReportTemplateSummaryDto;
import health.autoemplyserver.entity.ReportTemplate;
import health.autoemplyserver.repository.ReportTemplateRepository;
import health.autoemplyserver.service.report.ReportTemplateArchiveService;
import health.autoemplyserver.service.report.ReportTemplateContentReader;
import health.autoemplyserver.service.report.ReportTemplateContentReader.PreviewContent;
import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ReportTemplateService {

    private final ReportTemplateRepository reportTemplateRepository;
    private final ReportTemplateContentReader reportTemplateContentReader;
    private final ReportTemplateArchiveService reportTemplateArchiveService;

    public ReportTemplateService(
        ReportTemplateRepository reportTemplateRepository,
        ReportTemplateContentReader reportTemplateContentReader,
        ReportTemplateArchiveService reportTemplateArchiveService
    ) {
        this.reportTemplateRepository = reportTemplateRepository;
        this.reportTemplateContentReader = reportTemplateContentReader;
        this.reportTemplateArchiveService = reportTemplateArchiveService;
    }

    @Transactional(readOnly = true)
    public List<ReportTemplateSummaryDto> getAll() {
        return reportTemplateRepository.findAllByOrderByCategoryAscNameAsc().stream()
            .map(template -> new ReportTemplateSummaryDto(
                template.getId(),
                template.getName(),
                template.getCategory(),
                template.getOriginalFormName(),
                template.getPreviewData() != null,
                template.getPreviewContentType(),
                template.getCreatedAt(),
                template.getUpdatedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public ReportTemplateDetailDto get(UUID id) {
        ReportTemplate template = reportTemplateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template not found."));
        return new ReportTemplateDetailDto(
            template.getId(),
            template.getName(),
            template.getCategory(),
            template.getOriginalFormName(),
            template.getDfmContent(),
            template.getPasContent(),
            template.getPreviewData() != null,
            template.getPreviewContentType(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ReportTemplate getEntity(UUID id) {
        ReportTemplate template = reportTemplateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template not found."));
        if (template.getPreviewData() == null) {
            throw new NotFoundException("Preview not found.");
        }
        return template;
    }

    public ReportTemplateCreateResponse create(String name, String category, MultipartFile dfmFile, MultipartFile pasFile, MultipartFile previewFile) {
        if (name == null || name.isBlank() || category == null || category.isBlank()) {
            throw new BadRequestException("name and category are required.");
        }

        String dfmContent = reportTemplateContentReader.readRequiredText(dfmFile, "DFM");
        String pasContent = reportTemplateContentReader.readRequiredText(pasFile, "PAS");
        String originalFormName = reportTemplateContentReader.inferOriginalFormName(dfmFile);
        PreviewContent previewContent = reportTemplateContentReader.readPreview(previewFile);

        ReportTemplate template = new ReportTemplate();
        OffsetDateTime now = OffsetDateTime.now();
        template.setId(UUID.randomUUID());
        template.setName(name.trim());
        template.setCategory(category.trim());
        template.setDfmContent(dfmContent);
        template.setPasContent(pasContent);
        template.setOriginalFormName(originalFormName);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        template.setPreviewData(previewContent.data());
        template.setPreviewContentType(previewContent.contentType());

        reportTemplateRepository.save(template);
        return new ReportTemplateCreateResponse(template.getId(), template.getName(), template.getCategory(), template.getOriginalFormName());
    }

    public byte[] download(UUID id, String formName) {
        if (formName == null || formName.isBlank()) {
            throw new BadRequestException("formName is required.");
        }
        ReportTemplate template = reportTemplateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template not found."));
        return reportTemplateArchiveService.buildArchive(template, formName.trim());
    }

    public void delete(UUID id) {
        if (!reportTemplateRepository.existsById(id)) {
            throw new NotFoundException("Template not found.");
        }
        reportTemplateRepository.deleteById(id);
    }
}
