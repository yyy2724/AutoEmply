package health.autoemplyserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    //프롬프트 이름
    @Column(nullable = false)
    private String name;

    //프롬프트 내용
    @Column(name = "system_prompt", nullable = false, columnDefinition = "text")
    private String systemPrompt;

    //사용자 프롬프트 (사용안함)
    @Column(name = "user_prompt_template", nullable = false, columnDefinition = "text")
    private String userPromptTemplate;

    // 예시 템플릿 (룰 등) (사용안함)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style_rules_json", columnDefinition = "jsonb", nullable = true)
    private String styleRulesJson;

    // ai 모덱
    private String model;

    // AI 창의성
    private BigDecimal temperature;

    // AI 최대 토큰
    @Column(name = "max_tokens")
    private Integer maxTokens;

    //프리셋 사용/비사용 여부
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    //만든시간
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    //수정한 시간
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromptVersion> versions = new ArrayList<>();
}