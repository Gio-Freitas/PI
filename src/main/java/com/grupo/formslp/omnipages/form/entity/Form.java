package com.grupo.formslp.omnipages.form.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Getter 
@Setter 
@Entity 
@Table(name = "forms", schema = "landing")
public class Form {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "form_key", unique = true, nullable = false)
    private String formKey;

    @Column(name = "render_mode", nullable = false)
    private String renderMode = "CLASSIC";

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(name = "chatbot_greeting", columnDefinition = "TEXT")
    private String chatbotGreeting;

    @Column(name = "chatbot_farewell", columnDefinition = "TEXT")
    private String chatbotFarewell;

    @Column(name = "success_action", length = 50)
    private String sucessAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "success_config", columnDefinition = "jsonb")
    private Map<String, Object> successConfig;

    @Column(name = "consent_required", nullable = false)
    private Boolean consentRequired = false;

    @Column(columnDefinition = "TEXT")
    private String consent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_domains", columnDefinition = "jsonb")
    private List<String> allowedDomains;
    
    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormField> fields = new ArrayList<>();

    @CreationTimestamp 
    @Column(name= "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp 
    @Column(name="updated_at")
    private OffsetDateTime updatedAt;

    @Column(name="deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name="created_by")
    private UUID createdBy;

    @Column(name="updsated_by")
    private UUID updatedBy;
}
