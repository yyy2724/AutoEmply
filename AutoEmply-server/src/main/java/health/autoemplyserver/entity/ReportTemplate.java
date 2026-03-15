package health.autoemplyserver.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_templates")
public class ReportTemplate {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(name = "dfm_content", nullable = false, columnDefinition = "text")
    private String dfmContent;

    @Column(name = "pas_content", nullable = false, columnDefinition = "text")
    private String pasContent;

    @Column(name = "original_form_name", nullable = false)
    private String originalFormName;

    @Column(name = "preview_content_type")
    private String previewContentType;

    @Column(name = "preview_data", columnDefinition = "bytea")
    private byte[] previewData;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDfmContent() { return dfmContent; }
    public void setDfmContent(String dfmContent) { this.dfmContent = dfmContent; }
    public String getPasContent() { return pasContent; }
    public void setPasContent(String pasContent) { this.pasContent = pasContent; }
    public String getOriginalFormName() { return originalFormName; }
    public void setOriginalFormName(String originalFormName) { this.originalFormName = originalFormName; }
    public String getPreviewContentType() { return previewContentType; }
    public void setPreviewContentType(String previewContentType) { this.previewContentType = previewContentType; }
    public byte[] getPreviewData() { return previewData; }
    public void setPreviewData(byte[] previewData) { this.previewData = previewData; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
