package health.autoemplyserver.repository;

import health.autoemplyserver.entity.SampleTemplateSet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleTemplateSetRepository extends JpaRepository<SampleTemplateSet, UUID> {
}
