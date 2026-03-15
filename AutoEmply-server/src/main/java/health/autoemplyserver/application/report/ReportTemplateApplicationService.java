package health.autoemplyserver.application.report;

import health.autoemplyserver.dto.report.ReportTemplateCreateResponse;
import health.autoemplyserver.dto.report.ReportTemplateDetailDto;
import health.autoemplyserver.dto.report.ReportTemplateSummaryDto;
import health.autoemplyserver.entity.ReportTemplate;
import health.autoemplyserver.service.ReportTemplateService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportTemplateApplicationService {

    private final ReportTemplateService reportTemplateService;

    public ReportTemplateApplicationService(ReportTemplateService reportTemplateService) {
        this.reportTemplateService = reportTemplateService;
    }

    public List<ReportTemplateSummaryDto> getAll() {
        return reportTemplateService.getAll();
    }

    public ReportTemplateDetailDto get(UUID id) {
        return reportTemplateService.get(id);
    }

    public ReportTemplate getPreviewSource(UUID id) {
        return reportTemplateService.getEntity(id);
    }

    public ReportTemplateCreateResponse create(String name, String category, MultipartFile dfmFile, MultipartFile pasFile, MultipartFile previewFile) {
        return reportTemplateService.create(name, category, dfmFile, pasFile, previewFile);
    }

    public byte[] download(UUID id, String formName) {
        return reportTemplateService.download(id, formName);
    }

    public void delete(UUID id) {
        reportTemplateService.delete(id);
    }
}
