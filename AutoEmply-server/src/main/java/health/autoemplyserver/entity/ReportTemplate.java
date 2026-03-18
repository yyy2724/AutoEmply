package health.autoemplyserver.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "dfm_content", nullable = false, columnDefinition = "text")
    private String dfmContent;

    @Column(name = "pas_content", nullable = false, columnDefinition = "text")
    private String pasContent;

    @Column(name = "original_form_name", nullable = false, length = 255)
    private String originalFormName;

    @Column(name = "preview_content_type", length = 50)
    private String previewContentType;

    @Column(name = "preview_data", columnDefinition = "bytea")
    private byte[] previewData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

}
