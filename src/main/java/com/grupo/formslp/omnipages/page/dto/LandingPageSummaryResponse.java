package com.grupo.formslp.omnipages.page.dto;

import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

// linha da lista de landing pages (tela 1); segue o LandingPageResumo do contrato
// totalVisitas e totalEnvios saem nulos até existirem o rastreamento de visitas e o vínculo envio -> página
public record LandingPageSummaryResponse(
        UUID id,
        String nome,
        String slug,
        LandingPageStatus status,
        Long totalVisitas,
        Long totalEnvios,
        OffsetDateTime publicadaEm
) {

    public static LandingPageSummaryResponse from(LandingPage page) {
        return new LandingPageSummaryResponse(
                page.getId(),
                page.getName(),
                page.getSlug(),
                page.getStatus(),
                null,
                null,
                page.getPublishedAt()
        );
    }
}
