package com.grupo.formslp.omnipages.page.repository;

import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, UUID>, JpaSpecificationExecutor<LandingPage> {
    // Área administrativa, acessa após validação token JWT e de tenant
    // a listagem com filtros (tela 1) usa LandingPageSpecifications

    // busca uma página específica garantindo que ela pertence àquele tenant
    Optional<LandingPage> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    // o slug é único em toda a instalação, não só no tenant
    boolean existsBySlugAndDeletedAtIsNull(String slug);
    boolean existsBySlugAndIdNotAndDeletedAtIsNull(String slug, UUID id);

    // Área pública (acesso anon, sem token)
    // só a versão publicada é visível para o visitante
    Optional<LandingPage> findBySlugAndStatusAndDeletedAtIsNull(String slug, LandingPageStatus status);
}
