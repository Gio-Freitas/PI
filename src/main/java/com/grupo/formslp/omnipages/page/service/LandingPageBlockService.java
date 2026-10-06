package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.repository.FormRepository;
import com.grupo.formslp.omnipages.page.dto.BlockRequest;
import com.grupo.formslp.omnipages.page.dto.BlockResponse;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.exception.BlockValidationException;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// Blocos do rascunho de uma landing page (tela 2): listar, adicionar, editar, excluir, duplicar e reordenar.
// Toda alteração trava a página (findForUpdate), mantém as posições em 0..n-1 sem buracos
// e marca que o rascunho mudou desde a última publicação.
@Service
@RequiredArgsConstructor
public class LandingPageBlockService {

    static final String BLOCK_NOT_FOUND_KEY = "page.error.block-not-found";
    // limites contra abuso: o conteúdo vai inteiro para o HTML publicado
    static final int MAX_BLOCKS_PER_PAGE = 100;
    static final int MAX_CONTENT_BYTES = 32 * 1024;

    private final LandingPageService pageService;
    private final LandingPageRepository pageRepository;
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<BlockResponse> list(UUID tenantId, UUID pageId) {
        return pageService.findPage(tenantId, pageId).getBlocks().stream().map(BlockResponse::from).toList();
    }

    @Transactional
    public BlockResponse add(UUID tenantId, UUID pageId, BlockRequest request) {
        LandingPage page = lockPage(tenantId, pageId);
        ensureRoomForOneMore(page);
        LandingPageBlock block = new LandingPageBlock();
        block.setTenantId(tenantId);
        block.setPage(page);
        applyContent(block, tenantId, request);
        page.getBlocks().add(block);
        saveDraft(page);
        return BlockResponse.from(block);
    }

    @Transactional
    public BlockResponse update(UUID tenantId, UUID pageId, UUID blockId, BlockRequest request) {
        LandingPage page = lockPage(tenantId, pageId);
        LandingPageBlock block = findBlock(page, blockId);
        applyContent(block, tenantId, request);
        saveDraft(page);
        return BlockResponse.from(block);
    }

    @Transactional
    public void delete(UUID tenantId, UUID pageId, UUID blockId) {
        LandingPage page = lockPage(tenantId, pageId);
        page.getBlocks().remove(findBlock(page, blockId));
        saveDraft(page);
    }

    // a cópia entra logo depois do original
    @Transactional
    public BlockResponse duplicate(UUID tenantId, UUID pageId, UUID blockId) {
        LandingPage page = lockPage(tenantId, pageId);
        ensureRoomForOneMore(page);
        LandingPageBlock original = findBlock(page, blockId);
        LandingPageBlock copy = new LandingPageBlock();
        copy.setTenantId(tenantId);
        copy.setPage(page);
        copy.setType(original.getType());
        copy.setContent(deepCopy(original.getContent()));
        copy.setForm(original.getForm());
        page.getBlocks().add(page.getBlocks().indexOf(original) + 1, copy);
        saveDraft(page);
        return BlockResponse.from(copy);
    }

    // recebe todos os ids da página na nova ordem; lista incompleta, repetida ou com id de outra página é ORDEM_INVALIDA
    @Transactional
    public void reorder(UUID tenantId, UUID pageId, List<UUID> ids) {
        LandingPage page = lockPage(tenantId, pageId);
        Map<UUID, LandingPageBlock> byId = page.getBlocks().stream()
                .collect(Collectors.toMap(LandingPageBlock::getId, Function.identity()));
        if (ids.size() != byId.size() || !new HashSet<>(ids).equals(byId.keySet())) {
            throw new BlockValidationException("ids", "page.validation.invalid-order");
        }
        // ordena a própria coleção: tirar e recolocar os blocos faria o orphanRemoval apagá-los
        page.getBlocks().sort(Comparator.comparingInt(b -> ids.indexOf(b.getId())));
        saveDraft(page);
    }

    private LandingPage lockPage(UUID tenantId, UUID pageId) {
        return pageRepository.findForUpdate(pageId, tenantId)
                .orElseThrow(() -> new PageNotFoundException(LandingPageService.PAGE_NOT_FOUND_KEY));
    }

    private static void ensureRoomForOneMore(LandingPage page) {
        if (page.getBlocks().size() >= MAX_BLOCKS_PER_PAGE) {
            throw new BlockValidationException("blocos", "page.validation.too-many-blocks");
        }
    }

    private static LandingPageBlock findBlock(LandingPage page, UUID blockId) {
        return page.getBlocks().stream()
                .filter(b -> b.getId().equals(blockId))
                .findFirst()
                .orElseThrow(() -> new PageNotFoundException(BLOCK_NOT_FOUND_KEY));
    }

    private void applyContent(LandingPageBlock block, UUID tenantId, BlockRequest request) {
        if (serializedSize(request.conteudo()) > MAX_CONTENT_BYTES) {
            throw new BlockValidationException("conteudo", "page.validation.content-too-large");
        }
        block.setType(request.tipo());
        block.setContent(new LinkedHashMap<>(request.conteudo()));
        block.setForm(resolveForm(tenantId, request));
    }

    // bloco FORMULARIO exige um formulário do mesmo tenant; os outros tipos não podem apontar para um
    private Form resolveForm(UUID tenantId, BlockRequest request) {
        if (request.tipo() != BlockType.FORM) {
            if (request.formularioId() != null) {
                throw new BlockValidationException("formularioId", "page.validation.form-only-in-form-block");
            }
            return null;
        }
        if (request.formularioId() == null) {
            throw new BlockValidationException("formularioId", "page.validation.form-required");
        }
        return formRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.formularioId(), tenantId)
                .orElseThrow(() -> new BlockValidationException("formularioId", "page.validation.form-not-found"));
    }

    // renumera as posições pela ordem da lista e grava; depois dele, um bloco novo já tem id e posição.
    // A página já está gerenciada nesta transação, então basta o flush: um save() faria merge,
    // e o merge grava uma cópia do bloco novo, deixando o objeto que temos em mãos sem id
    private void saveDraft(LandingPage page) {
        List<LandingPageBlock> blocks = page.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setPosition(i);
        }
        page.setHasUnpublishedChanges(true);
        pageRepository.flush();
    }

    private int serializedSize(Map<String, Object> content) {
        return objectMapper.writeValueAsBytes(content).length;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> content) {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(content), LinkedHashMap.class);
    }
}
