package health.autoemplyserver.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prompt_presets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptPreset {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "text")
    private String systemPrompt;

    @Column(name = "user_prompt_template", nullable = false, columnDefinition = "text")
    private String userPromptTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style_rules_json", columnDefinition = "jsonb")
    private String styleRulesJson;

    @Column(name = "sample_template_ids_json", columnDefinition = "text")
    private String sampleTemplateIdsJson;

    @Column(name = "sample_template_set_id")
    private UUID sampleTemplateSetId;

    private String model;

    private BigDecimal temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromptVersion> versions = new ArrayList<>();
}
