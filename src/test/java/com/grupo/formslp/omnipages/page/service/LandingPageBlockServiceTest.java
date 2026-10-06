package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.repository.FormRepository;
import com.grupo.formslp.omnipages.page.dto.BlockRequest;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.exception.BlockValidationException;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// regras de validação dos blocos; o comportamento com o banco (trava, posições, ids) está no LandingPageBlockIntegrationTest
@ExtendWith(MockitoExtension.class)
class LandingPageBlockServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PAGE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID FORM_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    @Mock
    private LandingPageService pageService;
    @Mock
    private LandingPageRepository pageRepository;
    @Mock
    private FormRepository formRepository;

    private LandingPageBlockService service;
    private LandingPage page;

    @BeforeEach
    void setUp() {
        service = new LandingPageBlockService(pageService, pageRepository, formRepository, JsonMapper.builder().build());
        page = new LandingPage();
        page.setId(PAGE_ID);
        page.setTenantId(TENANT_ID);
        page.setHasUnpublishedChanges(false);
        lenient().when(pageRepository.findForUpdate(PAGE_ID, TENANT_ID)).thenReturn(Optional.of(page));
    }

    @Test
    void addAppendsAtTheEndAndMarksDraftAsChanged() {
        page.getBlocks().add(block(BlockType.HERO));

        service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.TEXT, Map.of("texto", "Olá"), null));

        assertEquals(2, page.getBlocks().size());
        assertEquals(BlockType.TEXT, page.getBlocks().get(1).getType());
        assertEquals(1, page.getBlocks().get(1).getPosition());
        assertTrue(page.getHasUnpublishedChanges());
        verify(pageRepository).flush();
    }

    @Test
    void formBlockRequiresAFormFromTheSameTenant() {
        when(formRepository.findByIdAndTenantIdAndDeletedAtIsNull(FORM_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertField("formularioId", () -> service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.FORM, Map.of(), null)));
        assertField("formularioId", () -> service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.FORM, Map.of(), FORM_ID)));
        assertTrue(page.getBlocks().isEmpty());
    }

    @Test
    void formBlockKeepsTheChosenForm() {
        Form form = new Form();
        form.setId(FORM_ID);
        when(formRepository.findByIdAndTenantIdAndDeletedAtIsNull(FORM_ID, TENANT_ID)).thenReturn(Optional.of(form));

        service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.FORM, Map.of("titulo", "Fale com a gente"), FORM_ID));

        assertEquals(form, page.getBlocks().get(0).getForm());
    }

    @Test
    void onlyFormBlockMayPointToAForm() {
        assertField("formularioId", () -> service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.TEXT, Map.of(), FORM_ID)));
        verify(formRepository, never()).findByIdAndTenantIdAndDeletedAtIsNull(FORM_ID, TENANT_ID);
    }

    @Test
    void contentAboveTheLimitIsRejected() {
        String big = "x".repeat(LandingPageBlockService.MAX_CONTENT_BYTES);

        assertField("conteudo", () -> service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.TEXT, Map.of("texto", big), null)));
    }

    @Test
    void pageIsLimitedToAHundredBlocks() {
        for (int i = 0; i < LandingPageBlockService.MAX_BLOCKS_PER_PAGE; i++) {
            page.getBlocks().add(block(BlockType.TEXT));
        }
        LandingPageBlock any = page.getBlocks().get(0);

        assertField("blocos", () -> service.add(TENANT_ID, PAGE_ID, new BlockRequest(BlockType.TEXT, Map.of(), null)));
        assertField("blocos", () -> service.duplicate(TENANT_ID, PAGE_ID, any.getId()));
    }

    @Test
    void duplicateGoesRightAfterTheOriginalWithAnIndependentContent() {
        LandingPageBlock first = block(BlockType.HERO);
        first.setContent(new java.util.LinkedHashMap<>(Map.of("itens", List.of("a", "b"))));
        LandingPageBlock second = block(BlockType.FOOTER);
        page.getBlocks().addAll(List.of(first, second));

        service.duplicate(TENANT_ID, PAGE_ID, first.getId());

        List<LandingPageBlock> blocks = page.getBlocks();
        assertEquals(List.of(BlockType.HERO, BlockType.HERO, BlockType.FOOTER), blocks.stream().map(LandingPageBlock::getType).toList());
        assertEquals(List.of(0, 1, 2), blocks.stream().map(LandingPageBlock::getPosition).toList());
        assertEquals(first.getContent(), blocks.get(1).getContent());
        assertNotSame(first.getContent().get("itens"), blocks.get(1).getContent().get("itens"));
    }

    @Test
    void reorderRequiresEveryBlockExactlyOnce() {
        LandingPageBlock a = block(BlockType.HERO);
        LandingPageBlock b = block(BlockType.TEXT);
        page.getBlocks().addAll(List.of(a, b));

        assertField("ids", () -> service.reorder(TENANT_ID, PAGE_ID, List.of(a.getId())));
        assertField("ids", () -> service.reorder(TENANT_ID, PAGE_ID, List.of(a.getId(), a.getId())));
        assertField("ids", () -> service.reorder(TENANT_ID, PAGE_ID, List.of(a.getId(), UUID.randomUUID())));

        service.reorder(TENANT_ID, PAGE_ID, List.of(b.getId(), a.getId()));
        assertEquals(List.of(b, a), page.getBlocks());
        assertEquals(List.of(0, 1), page.getBlocks().stream().map(LandingPageBlock::getPosition).toList());
    }

    @Test
    void blockOfAnotherPageIsNotFound() {
        page.getBlocks().add(block(BlockType.HERO));

        assertThrows(PageNotFoundException.class, () -> service.delete(TENANT_ID, PAGE_ID, UUID.randomUUID()));
        assertEquals(1, page.getBlocks().size());
    }

    @Test
    void pageOfAnotherTenantIsNotFound() {
        UUID otherTenant = UUID.randomUUID();
        when(pageRepository.findForUpdate(PAGE_ID, otherTenant)).thenReturn(Optional.empty());

        assertThrows(PageNotFoundException.class,
                () -> service.add(otherTenant, PAGE_ID, new BlockRequest(BlockType.TEXT, Map.of(), null)));
    }

    private static void assertField(String field, org.junit.jupiter.api.function.Executable call) {
        assertEquals(field, assertThrows(BlockValidationException.class, call).getField());
    }

    private LandingPageBlock block(BlockType type) {
        LandingPageBlock block = new LandingPageBlock();
        block.setId(UUID.randomUUID());
        block.setTenantId(TENANT_ID);
        block.setPage(page);
        block.setType(type);
        block.setPosition(page.getBlocks().size());
        return block;
    }
}
