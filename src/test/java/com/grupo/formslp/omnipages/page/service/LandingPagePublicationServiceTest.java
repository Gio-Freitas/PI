package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.enums.FormRenderMode;
import com.grupo.formslp.omnipages.form.enums.FormStatus;
import com.grupo.formslp.omnipages.page.dto.LandingPageResponse;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.exception.PageStateException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingPagePublicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PAGE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Mock
    private LandingPageRepository pageRepository;

    private LandingPagePublicationService service;
    private LandingPage page;

    @BeforeEach
    void setUp() {
        service = new LandingPagePublicationService(pageRepository, new LandingPageHtmlRenderer("/chatbot/index.html"));
        page = new LandingPage();
        page.setId(PAGE_ID);
        page.setTenantId(TENANT_ID);
        page.setName("Black Friday");
        page.setSlug("black-friday");
        page.setStatus(LandingPageStatus.DRAFT);
        lenient().when(pageRepository.findForUpdate(PAGE_ID, TENANT_ID)).thenReturn(Optional.of(page));
    }

    @Test
    void publishFreezesHtmlSeoAndFormKeys() {
        page.setSeo(new HashMap<>(Map.of("descricao", "Ofertas")));
        page.getBlocks().add(block(BlockType.HERO, Map.of("titulo", "Até 40%"), null));
        page.getBlocks().add(block(BlockType.FORM, Map.of(), form("contato", FormStatus.PUBLISHED)));
        page.getBlocks().add(block(BlockType.FORM, Map.of(), form("contato", FormStatus.PUBLISHED)));

        LandingPageResponse response = service.publish(TENANT_ID, PAGE_ID);

        assertEquals(LandingPageStatus.PUBLISHED, page.getStatus());
        assertFalse(page.getHasUnpublishedChanges());
        assertNotNull(page.getPublishedAt());
        assertTrue(page.getPublishedHtml().contains("<h1>Até 40%</h1>"));
        assertEquals(List.of("contato"), page.getPublishedFormKeys());
        assertEquals("Black Friday", page.getPublishedSeo().get("titulo"));
        assertEquals("Ofertas", page.getPublishedSeo().get("descricao"));
        assertEquals(true, page.getPublishedSeo().get("indexar"));
        assertEquals(LandingPageStatus.PUBLISHED, response.status());
    }

    @Test
    void chatbotFormIsValidatedButLeftOutOfTheFormJsList() {
        Form chat = form("chat", FormStatus.PUBLISHED);
        chat.setRenderMode(FormRenderMode.CHATBOT);
        page.getBlocks().add(block(BlockType.FORM, Map.of(), chat));
        page.getBlocks().add(block(BlockType.FORM, Map.of(), form("contato", FormStatus.PUBLISHED)));

        service.publish(TENANT_ID, PAGE_ID);

        assertEquals(List.of("contato"), page.getPublishedFormKeys());
        assertTrue(page.getPublishedHtml().contains("?form=chat"));

        chat.setStatus(FormStatus.DRAFT);
        assertCode("FORMULARIO_INATIVO", () -> service.publish(TENANT_ID, PAGE_ID));
    }

    @Test
    void republishingReplacesTheFrozenVersion() {
        page.getBlocks().add(block(BlockType.HERO, Map.of("titulo", "Versão 1"), null));
        service.publish(TENANT_ID, PAGE_ID);
        page.getBlocks().get(0).getContent().put("titulo", "Versão 2");

        service.publish(TENANT_ID, PAGE_ID);

        assertTrue(page.getPublishedHtml().contains("Versão 2"));
        assertFalse(page.getPublishedHtml().contains("Versão 1"));
    }

    @Test
    void publishRejectsArchivedEmptyOrInactiveFormPages() {
        page.setStatus(LandingPageStatus.ARCHIVED);
        page.getBlocks().add(block(BlockType.TEXT, Map.of("texto", "Oi"), null));
        assertCode("PAGINA_ARQUIVADA", () -> service.publish(TENANT_ID, PAGE_ID));

        page.setStatus(LandingPageStatus.DRAFT);
        page.getBlocks().clear();
        assertCode("PAGINA_SEM_BLOCOS", () -> service.publish(TENANT_ID, PAGE_ID));

        page.getBlocks().add(block(BlockType.FORM, Map.of(), form("rascunho", FormStatus.DRAFT)));
        assertCode("FORMULARIO_INATIVO", () -> service.publish(TENANT_ID, PAGE_ID));

        Form deleted = form("excluido", FormStatus.PUBLISHED);
        deleted.setDeletedAt(OffsetDateTime.now());
        page.getBlocks().set(0, block(BlockType.FORM, Map.of(), deleted));
        assertCode("FORMULARIO_INATIVO", () -> service.publish(TENANT_ID, PAGE_ID));

        assertEquals(LandingPageStatus.DRAFT, page.getStatus());
        assertNull(page.getPublishedHtml());
    }

    @Test
    void unpublishTakesThePageOfflineAndOnlyWorksWhenPublished() {
        assertCode("PAGINA_NAO_PUBLICADA", () -> service.unpublish(TENANT_ID, PAGE_ID));
        page.getBlocks().add(block(BlockType.TEXT, Map.of("texto", "Oi"), null));
        service.publish(TENANT_ID, PAGE_ID);

        service.unpublish(TENANT_ID, PAGE_ID);

        assertEquals(LandingPageStatus.DRAFT, page.getStatus());
        assertNull(page.getPublishedHtml());
        assertNull(page.getPublishedAt());
        assertTrue(page.getHasUnpublishedChanges());
        assertEquals(1, page.getBlocks().size());
    }

    @Test
    void archiveTakesOfflineAndIsIdempotent() {
        page.getBlocks().add(block(BlockType.TEXT, Map.of("texto", "Oi"), null));
        service.publish(TENANT_ID, PAGE_ID);

        service.archive(TENANT_ID, PAGE_ID);
        service.archive(TENANT_ID, PAGE_ID);

        assertEquals(LandingPageStatus.ARCHIVED, page.getStatus());
        assertNull(page.getPublishedHtml());
    }

    @Test
    void publicReadOnlyFindsPublishedPages() {
        when(pageRepository.findBySlugAndStatusAndDeletedAtIsNull("rascunho", LandingPageStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThrows(PageNotFoundException.class, () -> service.getPublished("rascunho"));
    }

    @Test
    void pageOfAnotherTenantIsNotFound() {
        UUID otherTenant = UUID.randomUUID();
        when(pageRepository.findForUpdate(PAGE_ID, otherTenant)).thenReturn(Optional.empty());

        assertThrows(PageNotFoundException.class, () -> service.publish(otherTenant, PAGE_ID));
    }

    private static void assertCode(String code, org.junit.jupiter.api.function.Executable call) {
        assertEquals(code, assertThrows(PageStateException.class, call).getCode());
    }

    private static Form form(String key, FormStatus status) {
        Form form = new Form();
        form.setId(UUID.randomUUID());
        form.setFormKey(key);
        form.setStatus(status);
        return form;
    }

    private LandingPageBlock block(BlockType type, Map<String, Object> content, Form form) {
        LandingPageBlock block = new LandingPageBlock();
        block.setId(UUID.randomUUID());
        block.setPage(page);
        block.setType(type);
        block.setContent(new HashMap<>(content));
        block.setForm(form);
        return block;
    }
}
