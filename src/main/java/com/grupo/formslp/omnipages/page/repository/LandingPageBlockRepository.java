package com.grupo.formslp.omnipages.page.repository;

import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LandingPageBlockRepository extends JpaRepository<LandingPageBlock, UUID> {

    // blocos do rascunho de uma página, na ordem de exibição
    List<LandingPageBlock> findAllByPageIdAndTenantIdOrderByPositionAsc(UUID pageId, UUID tenantId);
    // busca um bloco garantindo que ele é daquela página e daquele tenant
    Optional<LandingPageBlock> findByIdAndPageIdAndTenantId(UUID id, UUID pageId, UUID tenantId);
}
