package health.autoemplyserver.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "prompt_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptVersion {

    @Id
    private UUID id;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "text")
    private String systemPrompt;

    @Column(name = "user_prompt_template", nullable = false, columnDefinition = "text")
    private String userPromptTemplate;

    @Column(name = "style_rules_json", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String styleRulesJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preset_id", nullable = false)
    private PromptPreset preset;
}
