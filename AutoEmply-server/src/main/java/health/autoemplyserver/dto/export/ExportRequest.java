package health.autoemplyserver.dto.export;

import health.autoemplyserver.model.LayoutSpec;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExportRequest(
    @NotBlank String formName,
    @NotNull LayoutSpec layoutSpec
) {
}
