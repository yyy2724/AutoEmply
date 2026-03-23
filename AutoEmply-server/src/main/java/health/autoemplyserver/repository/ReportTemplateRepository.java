package health.autoemplyserver.repository;

import health.autoemplyserver.entity.ReportTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, UUID> {

    List<ReportTemplate> findAllByOrderByCategoryAscNameAsc();

    List<ReportTemplate> findAllByIdIn(List<UUID> ids);
}
