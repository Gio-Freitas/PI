package com.grupo.formslp.omnipages.page.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "landing_pages", schema = "landing")
public class LandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LandingPageStatus status = LandingPageStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> seo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> tracking;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> theme;

    // rascunho mudou depois da última publicação
    @Column(name = "has_unpublished_changes", nullable = false)
    private Boolean hasUnpublishedChanges = true;

    // versão congelada, gerada na publicação; edições posteriores não mexem nela
    @Column(name = "published_html", columnDefinition = "TEXT")
    private String publishedHtml;

    // SEO congelado junto com o HTML: editar o SEO do rascunho não muda o que está no ar
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "published_seo", columnDefinition = "jsonb")
    private Map<String, Object> publishedSeo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "published_form_keys", columnDefinition = "jsonb")
    private List<String> publishedFormKeys;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    // blocos do rascunho, na ordem de exibição
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<LandingPageBlock> blocks = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
