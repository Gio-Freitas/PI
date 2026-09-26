package com.grupo.formslp.omnipages.form.repository;

import com.grupo.formslp.omnipages.form.entity.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository 
public interface FormRepository extends JpaRepository<Form, UUID>{
    // Área administrativa, acessa após validação token JWT e de tenant

    // lista todos os forms do cliente, paginados, ignorando os deletados
    Page<Form> findAllByTenantIdAndDeleteAtIsNull(UUID tenantId, Pageable pageable);
    // busca um form específico garantindo que ele pertence àquele tenant
    Optional<Form> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Área pública (acesso anon, sem token)
    // busca o formulário a partir de URL pública sem vazar o ID real
    Optional<Form> findByFormKeyAndDeletedAtIsNull(String formKey);
}
