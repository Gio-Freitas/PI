package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.enums.FormRenderMode;
import com.grupo.formslp.omnipages.form.enums.FormStatus;
import com.grupo.formslp.omnipages.form.repository.FormRepository;
import com.grupo.formslp.omnipages.page.dto.BlockRequest;
import com.grupo.formslp.omnipages.page.dto.BlockResponse;
import com.grupo.formslp.omnipages.page.dto.LandingPageRequest;
import com.grupo.formslp.omnipages.page.dto.PublishedPageResponse;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// publicação com o banco de verdade: o que vai ao ar é congelado e a área pública só enxerga a versão publicada.
// Roda contra o Postgres configurado, como o OmnipagesApplicationTests
@SpringBootTest
class LandingPagePublicationIntegrationTest {

    @Autowired
    private LandingPageService pageService;
    @Autowired
    private LandingPageBlockService blockService;
    @Autowired
    private LandingPagePublicationService publicationService;
    @Autowired
    private LandingPageRepository pageRepository;
    @Autowired
    private FormRepository formRepository;

    private final UUID tenantId = UUID.randomUUID();
    private final String slug = "teste-publicacao-" + UUID.randomUUID();
    private UUID pageId;
    private Form form;

    @BeforeEach
    void setUp() {
        form = new Form();
        form.setTenantId(tenantId);
        form.setName("Contato");
        form.setFormKey("contato-" + UUID.randomUUID());
        form.setRenderMode(FormRenderMode.CLASSIC);
        form.setStatus(FormStatus.PUBLISHED);
        form = formRepository.save(form);
        pageId = pageService.create(tenantId, new LandingPageRequest("Black Friday", slug,
                new LandingPageRequest.Seo("Ofertas", null, null, null), null, null)).id();
    }

    @AfterEach
    void cleanUp() {
        pageRepository.deleteById(pageId);
        formRepository.deleteById(form.getId());
    }

    @Test
    void publishedVersionStaysFrozenWhileTheDraftChanges() {
        BlockResponse hero = blockService.add(tenantId, pageId,
                new BlockRequest(BlockType.HERO, Map.of("titulo", "Até 40%"), null));
        blockService.add(tenantId, pageId, new BlockRequest(BlockType.FORM, Map.of(), form.getId()));

        publicationService.publish(tenantId, pageId);
        blockService.update(tenantId, pageId, hero.id(),
                new BlockRequest(BlockType.HERO, Map.of("titulo", "Rascunho novo"), null));

        PublishedPageResponse live = publicationService.getPublished(slug);
        assertTrue(live.html().contains("<h1>Até 40%</h1>"));
        assertFalse(live.html().contains("Rascunho novo"));
        assertEquals(List.of(form.getFormKey()), live.formularios());
        assertEquals("Ofertas", live.seo().get("titulo"));
        assertTrue(pageService.get(tenantId, pageId).temAlteracaoNaoPublicada());
    }

    @Test
    void unpublishedPageDisappearsFromThePublicArea() {
        blockService.add(tenantId, pageId, new BlockRequest(BlockType.TEXT, Map.of("texto", "Oi"), null));
        publicationService.publish(tenantId, pageId);

        publicationService.unpublish(tenantId, pageId);

        assertThrows(PageNotFoundException.class, () -> publicationService.getPublished(slug));
    }
}
