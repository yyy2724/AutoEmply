package health.autoemplyserver.service.report;

import health.autoemplyserver.entity.ReportTemplate;
import health.autoemplyserver.service.DelphiRenamer;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateArchiveService {

    private final DelphiRenamer delphiRenamer;

    public ReportTemplateArchiveService(DelphiRenamer delphiRenamer) {
        this.delphiRenamer = delphiRenamer;
    }

    public byte[] buildArchive(ReportTemplate template, String targetFormName) {
        String internalName = delphiRenamer.extractFormNameFromDfm(template.getDfmContent());
        if (internalName == null || internalName.isBlank()) {
            internalName = template.getOriginalFormName();
        }
        return delphiRenamer.renameAndZip(
            template.getOriginalFormName(),
            internalName,
            targetFormName,
            template.getDfmContent(),
            template.getPasContent()
        );
    }
}
