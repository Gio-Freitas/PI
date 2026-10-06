package com.grupo.formslp.omnipages.page.dto;

import com.grupo.formslp.omnipages.page.entity.LandingPage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

// versão publicada da página, como a área pública recebe (tela 12); segue o PaginaPublicada do contrato.
// Tudo aqui é o que foi congelado na publicação, nunca o rascunho
public record PublishedPageResponse(
        String slug,
        Map<String, Object> seo,
        String html,
        List<String> formularios,
        OffsetDateTime publicadaEm
) {

    public static PublishedPageResponse from(LandingPage page) {
        return new PublishedPageResponse(
                page.getSlug(),
                page.getPublishedSeo(),
                page.getPublishedHtml(),
                page.getPublishedFormKeys() == null ? List.of() : page.getPublishedFormKeys(),
                page.getPublishedAt()
        );
    }
}
