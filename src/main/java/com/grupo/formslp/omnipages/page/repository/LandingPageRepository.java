package com.grupo.formslp.omnipages.page.repository;

import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, UUID> {
    // Área administrativa, acessa após validação token JWT e de tenant

    // lista as páginas do cliente, paginadas, ignorando as excluídas
    Page<LandingPage> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);
    // mesma lista, filtrada por status (tela 1)
    Page<LandingPage> findAllByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, LandingPageStatus status, Pageable pageable);
    // busca uma página específica garantindo que ela pertence àquele tenant
    Optional<LandingPage> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    // o slug é único em toda a instalação, não só no tenant
    boolean existsBySlugAndDeletedAtIsNull(String slug);
    boolean existsBySlugAndIdNotAndDeletedAtIsNull(String slug, UUID id);

    // Área pública (acesso anon, sem token)
    // só a versão publicada é visível para o visitante
    Optional<LandingPage> findBySlugAndStatusAndDeletedAtIsNull(String slug, LandingPageStatus status);
}
