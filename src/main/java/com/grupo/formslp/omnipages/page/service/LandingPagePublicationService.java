package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.form.entity.Form;
import com.grupo.formslp.omnipages.form.enums.FormStatus;
import com.grupo.formslp.omnipages.page.dto.LandingPageResponse;
import com.grupo.formslp.omnipages.page.dto.PublishedPageResponse;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.exception.PageStateException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Ciclo de vida da landing page: RASCUNHO -> PUBLICADA (gera o HTML congelado) -> RASCUNHO ou ARQUIVADA,
// e a leitura pública da versão publicada (tela 12). Toda mudança de estado trava a página (findForUpdate),
// para a publicação não gerar o HTML no meio de uma edição de blocos.
@Service
@RequiredArgsConstructor
public class LandingPagePublicationService {

    static final String PUBLIC_PAGE_NOT_FOUND_KEY = "page.error.public-page-not-found";

    private final LandingPageRepository pageRepository;
    private final LandingPageHtmlRenderer renderer;

    @Transactional
    public LandingPageResponse publish(UUID tenantId, UUID pageId) {
        LandingPage page = lockPage(tenantId, pageId);
        if (page.getStatus() == LandingPageStatus.ARCHIVED) {
            throw new PageStateException("PAGINA_ARQUIVADA", "page.state.archived");
        }
        if (page.getBlocks().isEmpty()) {
            throw new PageStateException("PAGINA_SEM_BLOCOS", "page.state.no-blocks");
        }
        List<String> formKeys = activeFormKeys(page.getBlocks());

        Map<String, Object> seo = frozenSeo(page);
        page.setPublishedSeo(seo);
        page.setPublishedHtml(renderer.render(page, seo));
        page.setPublishedFormKeys(formKeys);
        page.setPublishedAt(OffsetDateTime.now());
        page.setStatus(LandingPageStatus.PUBLISHED);
        page.setHasUnpublishedChanges(false);
        pageRepository.flush();
        return LandingPageResponse.from(page);
    }

    // tira do ar e volta a ser rascunho; o rascunho continua intacto para uma nova publicação
    @Transactional
    public LandingPageResponse unpublish(UUID tenantId, UUID pageId) {
        LandingPage page = lockPage(tenantId, pageId);
        if (page.getStatus() != LandingPageStatus.PUBLISHED) {
            throw new PageStateException("PAGINA_NAO_PUBLICADA", "page.state.not-published");
        }
        takeOffline(page, LandingPageStatus.DRAFT);
        return LandingPageResponse.from(page);
    }

    // tira do ar (se estiver) e bloqueia nova publicação; arquivar de novo não muda nada
    @Transactional
    public LandingPageResponse archive(UUID tenantId, UUID pageId) {
        LandingPage page = lockPage(tenantId, pageId);
        if (page.getStatus() != LandingPageStatus.ARCHIVED) {
            takeOffline(page, LandingPageStatus.ARCHIVED);
        }
        return LandingPageResponse.from(page);
    }

    // Área pública (acesso anon, sem token): o tenant sai do slug; rascunho, arquivada ou excluída é 404
    @Transactional(readOnly = true)
    public PublishedPageResponse getPublished(String slug) {
        return pageRepository.findBySlugAndStatusAndDeletedAtIsNull(slug, LandingPageStatus.PUBLISHED)
                .map(PublishedPageResponse::from)
                .orElseThrow(() -> new PageNotFoundException(PUBLIC_PAGE_NOT_FOUND_KEY));
    }

    private LandingPage lockPage(UUID tenantId, UUID pageId) {
        return pageRepository.findForUpdate(pageId, tenantId)
                .orElseThrow(() -> new PageNotFoundException(LandingPageService.PAGE_NOT_FOUND_KEY));
    }

    // cada bloco FORMULARIO precisa de um formulário ativo (publicado e não excluído), o mesmo critério
    // que a área pública usa para aceitar envios. Devolve, sem repetição e na ordem da página, as chaves
    // que o form.js vai montar; o chatbot não entra porque abre pelo botão flutuante, não pelo form.js
    private static List<String> activeFormKeys(List<LandingPageBlock> blocks) {
        List<LandingPageBlock> formBlocks = blocks.stream().filter(b -> b.getType() == BlockType.FORM).toList();
        for (LandingPageBlock block : formBlocks) {
            Form form = block.getForm();
            if (form == null || form.getDeletedAt() != null || form.getStatus() != FormStatus.PUBLISHED) {
                throw new PageStateException("FORMULARIO_INATIVO", "page.state.form-inactive");
            }
        }
        return formBlocks.stream()
                .filter(b -> !LandingPageHtmlRenderer.isChatbot(b))
                .map(b -> b.getForm().getFormKey())
                .distinct()
                .toList();
    }

    // sem título de SEO, a aba do navegador mostra o nome da página
    private static Map<String, Object> frozenSeo(LandingPage page) {
        Map<String, Object> draft = page.getSeo() == null ? Map.of() : page.getSeo();
        Map<String, Object> seo = new LinkedHashMap<>();
        Object title = draft.get("titulo");
        seo.put("titulo", title instanceof String s && !s.isBlank() ? s : page.getName());
        seo.put("descricao", draft.get("descricao"));
        seo.put("imagemCompartilhamento", draft.get("imagemCompartilhamento"));
        seo.put("indexar", !Boolean.FALSE.equals(draft.get("indexar")));
        return seo;
    }

    private void takeOffline(LandingPage page, LandingPageStatus newStatus) {
        page.setStatus(newStatus);
        page.setPublishedHtml(null);
        page.setPublishedSeo(null);
        page.setPublishedFormKeys(null);
        page.setPublishedAt(null);
        page.setHasUnpublishedChanges(true);
        pageRepository.flush();
    }
}
