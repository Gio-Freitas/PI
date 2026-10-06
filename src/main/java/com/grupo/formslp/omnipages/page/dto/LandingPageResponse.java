package com.grupo.formslp.omnipages.page.dto;

import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// página completa, com os blocos do rascunho; segue o LandingPage do contrato
public record LandingPageResponse(
        UUID id,
        String nome,
        String slug,
        LandingPageStatus status,
        Map<String, Object> seo,
        Map<String, Object> rastreamento,
        Map<String, Object> tema,
        OffsetDateTime publicadaEm,
        boolean temAlteracaoNaoPublicada,
        List<BlockResponse> blocos,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm,
        UUID criadoPor,
        UUID atualizadoPor
) {

    public static LandingPageResponse from(LandingPage page) {
        return new LandingPageResponse(
                page.getId(),
                page.getName(),
                page.getSlug(),
                page.getStatus(),
                page.getSeo(),
                page.getTracking(),
                page.getTheme(),
                page.getPublishedAt(),
                Boolean.TRUE.equals(page.getHasUnpublishedChanges()),
                page.getBlocks().stream().map(BlockResponse::from).toList(),
                page.getCreatedAt(),
                page.getUpdatedAt(),
                page.getCreatedBy(),
                page.getUpdatedBy()
        );
    }
}
