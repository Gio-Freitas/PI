package com.grupo.formslp.omnipages.page.repository;

import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, UUID>, JpaSpecificationExecutor<LandingPage> {
    // Área administrativa, acessa após validação token JWT e de tenant
    // a listagem com filtros (tela 1) usa LandingPageSpecifications

    // busca uma página específica garantindo que ela pertence àquele tenant
    Optional<LandingPage> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    // mesma busca, travando a linha até o fim da transação (SELECT ... FOR UPDATE): quem mexe nos blocos
    // de uma página espera o outro terminar, para dois blocos não ganharem a mesma posição
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LandingPage p where p.id = :id and p.tenantId = :tenantId and p.deletedAt is null")
    Optional<LandingPage> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
    // o slug é único em toda a instalação, não só no tenant
    boolean existsBySlugAndDeletedAtIsNull(String slug);
    boolean existsBySlugAndIdNotAndDeletedAtIsNull(String slug, UUID id);

    // Área pública (acesso anon, sem token)
    // só a versão publicada é visível para o visitante
    Optional<LandingPage> findBySlugAndStatusAndDeletedAtIsNull(String slug, LandingPageStatus status);
}
