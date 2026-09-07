package com.grupo.formslp.omnipages.core.repository;

import com.grupo.formslp.omnipages.core.domain.FormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository 
public interface FormFieldRepository extends JpaRepository<FormField, UUID>{
    // traz todos os campos de um formulário ordenados pela posição visual
    List<FormField> findAllByFormIdAndTenantIdAndDeletedAtIsNullOrderByPositionAsc(UUID formId, UUID tenantId);
    // busca um campo específico para edição/exclusão
    Optional<FormField> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    
}
