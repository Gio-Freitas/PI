package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.page.dto.LandingPageRequest;
import com.grupo.formslp.omnipages.page.dto.LandingPageResponse;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import com.grupo.formslp.omnipages.page.exception.InvalidQueryParameterException;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.exception.SlugAlreadyInUseException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandingPageServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PAGE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Mock
    private LandingPageRepository pageRepository;

    private LandingPageService service;

    @BeforeEach
    void setUp() {
        service = new LandingPageService(pageRepository);
        lenient().when(pageRepository.save(any(LandingPage.class))).thenAnswer(invocation -> {
            LandingPage page = invocation.getArgument(0);
            if (page.getId() == null) {
                page.setId(PAGE_ID);
            }
            return page;
        });
    }

    @Test
    void createStartsAsDraftWithUnpublishedChangesAndContractFields() {
        LandingPageResponse response = service.create(TENANT_ID, request("black-friday-2026"));

        ArgumentCaptor<LandingPage> saved = ArgumentCaptor.forClass(LandingPage.class);
        verify(pageRepository).save(saved.capture());
        verify(pageRepository).flush();
        LandingPage page = saved.getValue();
        assertEquals(TENANT_ID, page.getTenantId());
        assertEquals(LandingPageStatus.DRAFT, page.getStatus());
        assertTrue(page.getHasUnpublishedChanges());
        assertEquals("Black Friday", page.getName());
        assertEquals("Ofertas", page.getSeo().get("titulo"));
        assertEquals(true, page.getSeo().get("indexar"));
        assertEquals("#2563EB", page.getTheme().get("corPrimaria"));
        assertEquals(PAGE_ID, response.id());
        assertNull(response.publicadaEm());
    }

    @Test
    void createRejectsSlugAlreadyInUse() {
        when(pageRepository.existsBySlugAndDeletedAtIsNull("black-friday-2026")).thenReturn(true);

        assertThrows(SlugAlreadyInUseException.class, () -> service.create(TENANT_ID, request("black-friday-2026")));
        verify(pageRepository, never()).save(any());
    }

    @Test
    void createTurnsUniqueIndexViolationIntoSlugConflict() {
        // duas requisições simultâneas passam pela checagem; o índice único do banco recusa a segunda
        doThrow(new DataIntegrityViolationException("violates unique constraint \"uq_landing_pages_slug\""))
                .when(pageRepository).flush();

        assertThrows(SlugAlreadyInUseException.class, () -> service.create(TENANT_ID, request("black-friday-2026")));
    }

    @Test
    void createDoesNotHideOtherIntegrityErrors() {
        doThrow(new DataIntegrityViolationException("null value in column \"name\""))
                .when(pageRepository).flush();

        assertThrows(DataIntegrityViolationException.class, () -> service.create(TENANT_ID, request("black-friday-2026")));
    }

    @Test
    void getFromAnotherTenantIsNotFound() {
        UUID otherTenant = UUID.randomUUID();
        when(pageRepository.findByIdAndTenantIdAndDeletedAtIsNull(PAGE_ID, otherTenant)).thenReturn(Optional.empty());

        assertThrows(PageNotFoundException.class, () -> service.get(otherTenant, PAGE_ID));
    }

    @Test
    void updateKeepsPublishedVersionAndMarksDraftAsChanged() {
        LandingPage page = existingPage();
        page.setStatus(LandingPageStatus.PUBLISHED);
        page.setPublishedHtml("<html>versão no ar</html>");
        page.setHasUnpublishedChanges(false);
        when(pageRepository.findForUpdate(PAGE_ID, TENANT_ID)).thenReturn(Optional.of(page));

        LandingPageResponse response = service.update(TENANT_ID, PAGE_ID, request("black-friday-2026"));

        assertEquals(LandingPageStatus.PUBLISHED, page.getStatus());
        assertEquals("<html>versão no ar</html>", page.getPublishedHtml());
        assertTrue(response.temAlteracaoNaoPublicada());
    }

    @Test
    void updateRejectsSlugOfAnotherPage() {
        when(pageRepository.findForUpdate(PAGE_ID, TENANT_ID)).thenReturn(Optional.of(existingPage()));
        when(pageRepository.existsBySlugAndIdNotAndDeletedAtIsNull("ja-existe", PAGE_ID)).thenReturn(true);

        assertThrows(SlugAlreadyInUseException.class, () -> service.update(TENANT_ID, PAGE_ID, request("ja-existe")));
    }

    @Test
    void deleteIsLogical() {
        LandingPage page = existingPage();
        when(pageRepository.findForUpdate(PAGE_ID, TENANT_ID)).thenReturn(Optional.of(page));

        service.delete(TENANT_ID, PAGE_ID);

        assertNotNull(page.getDeletedAt());
        verify(pageRepository).flush();
        verify(pageRepository, never()).delete(any(LandingPage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listTranslatesContractNamesToEntity() {
        when(pageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    assertEquals(Sort.by(Sort.Direction.DESC, "publishedAt"), pageable.getSort());
                    return new PageImpl<>(List.of(existingPage()), pageable, 1);
                });

        var result = service.list(TENANT_ID, null, "RASCUNHO", 0, 20, "publicadaEm,desc");

        assertEquals(1, result.total());
        assertEquals("black-friday-2026", result.itens().get(0).slug());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listSortsAscendingWhenDirectionIsOmittedAndClampsPageSize() {
        when(pageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    assertEquals(Sort.by(Sort.Direction.ASC, "name"), pageable.getSort());
                    assertEquals(100, pageable.getPageSize());
                    assertEquals(0, pageable.getPageNumber());
                    return Page.empty(pageable);
                });

        service.list(TENANT_ID, null, null, -3, 500, "nome");
    }

    @Test
    void listRejectsValuesOutsideTheContract() {
        assertThrows(InvalidQueryParameterException.class, () -> service.list(TENANT_ID, null, "QUALQUER", 0, 20, null));
        assertThrows(InvalidQueryParameterException.class, () -> service.list(TENANT_ID, null, null, 0, 20, "tenantId,asc"));
        assertThrows(InvalidQueryParameterException.class, () -> service.list(TENANT_ID, null, null, 0, 20, "nome,subindo"));
        assertThrows(InvalidQueryParameterException.class, () -> service.list(TENANT_ID, null, null, 0, 20, "nome,asc,extra"));
    }

    private static LandingPageRequest request(String slug) {
        return new LandingPageRequest(
                "  Black Friday  ",
                slug,
                new LandingPageRequest.Seo("Ofertas", "Até 40% de desconto", null, null),
                Map.of("gtm", "GTM-123"),
                new LandingPageRequest.Tema("#2563EB", "#FFFFFF", "Inter"));
    }

    private static LandingPage existingPage() {
        LandingPage page = new LandingPage();
        page.setId(PAGE_ID);
        page.setTenantId(TENANT_ID);
        page.setName("Black Friday");
        page.setSlug("black-friday-2026");
        page.setStatus(LandingPageStatus.DRAFT);
        page.setCreatedAt(OffsetDateTime.now());
        return page;
    }
}
