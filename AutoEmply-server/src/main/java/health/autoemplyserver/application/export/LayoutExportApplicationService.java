package health.autoemplyserver.application.export;

import health.autoemplyserver.dto.export.ExportRequest;
import health.autoemplyserver.service.DelphiGenerator;
import health.autoemplyserver.service.LayoutSpecValidator;
import health.autoemplyserver.support.exception.BadRequestException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LayoutExportApplicationService {

    private final DelphiGenerator delphiGenerator;
    private final LayoutSpecValidator layoutSpecValidator;

    public LayoutExportApplicationService(DelphiGenerator delphiGenerator, LayoutSpecValidator layoutSpecValidator) {
        this.delphiGenerator = delphiGenerator;
        this.layoutSpecValidator = layoutSpecValidator;
    }

    public byte[] export(ExportRequest request) {
        String formName = request.formName().trim();
        List<String> errors = layoutSpecValidator.validate(formName, request.layoutSpec());
        if (!errors.isEmpty()) {
            throw new BadRequestException("Invalid request", errors);
        }
        return delphiGenerator.generateZip(formName, request.layoutSpec());
    }
}
