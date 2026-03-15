package health.autoemplyserver.repository;

import health.autoemplyserver.entity.PromptVersion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, UUID> {
}
