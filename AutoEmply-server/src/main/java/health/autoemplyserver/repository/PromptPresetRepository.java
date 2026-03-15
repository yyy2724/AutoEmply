package health.autoemplyserver.repository;

import health.autoemplyserver.entity.PromptPreset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptPresetRepository extends JpaRepository<PromptPreset, UUID> {

    List<PromptPreset> findAllByOrderByActiveDescNameAsc();

    List<PromptPreset> findByActiveTrueOrderByUpdatedAtDesc();

    List<PromptPreset> findByActiveTrueAndIdNotOrderByUpdatedAtDesc(UUID id);

    Optional<PromptPreset> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
