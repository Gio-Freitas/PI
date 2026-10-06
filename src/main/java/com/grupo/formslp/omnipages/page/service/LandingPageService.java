package com.grupo.formslp.omnipages.page.service;

import com.grupo.formslp.omnipages.page.dto.LandingPageRequest;
import com.grupo.formslp.omnipages.page.dto.LandingPageResponse;
import com.grupo.formslp.omnipages.page.dto.LandingPageSummaryResponse;
import com.grupo.formslp.omnipages.page.dto.PageResult;
import com.grupo.formslp.omnipages.page.entity.LandingPage;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import com.grupo.formslp.omnipages.page.exception.InvalidQueryParameterException;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.exception.SlugAlreadyInUseException;
import com.grupo.formslp.omnipages.page.repository.LandingPageRepository;
import com.grupo.formslp.omnipages.page.repository.LandingPageSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Área administrativa das landing pages (telas 1 e 3): listar, criar, detalhar, editar configurações e excluir
@Service
@RequiredArgsConstructor
public class LandingPageService {

    static final String PAGE_NOT_FOUND_KEY = "page.error.page-not-found";
    private static final String SLUG_UNIQUE_INDEX = "uq_landing_pages_slug";

    // campos aceitos no ?ordenar=campo,direcao, com o nome do contrato -> atributo da entidade
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "nome", "name",
            "status", "status",
            "criadoEm", "createdAt",
            "atualizadoEm", "updatedAt",
            "publicadaEm", "publishedAt"
    );
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final LandingPageRepository pageRepository;

    @Transactional(readOnly = true)
    public PageResult<LandingPageSummaryResponse> list(UUID tenantId, String name, String status,
                                                       int page, int size, String sort) {
        LandingPageStatus statusFilter = status == null || status.isBlank() ? null
                : LandingPageStatus.fromContractName(status)
                        .orElseThrow(() -> new InvalidQueryParameterException("status: " + status));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), parseSort(sort));
        return PageResult.from(
                pageRepository.findAll(LandingPageSpecifications.list(tenantId, name, statusFilter), pageable),
                LandingPageSummaryResponse::from);
    }

    @Transactional
    public LandingPageResponse create(UUID tenantId, LandingPageRequest request) {
        if (pageRepository.existsBySlugAndDeletedAtIsNull(request.slug())) {
            throw new SlugAlreadyInUseException(request.slug());
        }
        LandingPage page = new LandingPage();
        page.setTenantId(tenantId);
        page.setStatus(LandingPageStatus.DRAFT);
        applySettings(page, request);
        return LandingPageResponse.from(flushCheckingSlug(pageRepository.save(page)));
    }

    @Transactional(readOnly = true)
    public LandingPageResponse get(UUID tenantId, UUID pageId) {
        return LandingPageResponse.from(findPage(tenantId, pageId));
    }

    // editar configurações não mexe na versão publicada: só marca que o rascunho mudou.
    // Trava a página como a publicação faz: o Hibernate grava todas as colunas, e um PUT lido antes
    // de uma publicação e gravado depois dela tiraria a página do ar
    @Transactional
    public LandingPageResponse update(UUID tenantId, UUID pageId, LandingPageRequest request) {
        LandingPage page = lockPage(tenantId, pageId);
        if (pageRepository.existsBySlugAndIdNotAndDeletedAtIsNull(request.slug(), pageId)) {
            throw new SlugAlreadyInUseException(request.slug());
        }
        applySettings(page, request);
        page.setHasUnpublishedChanges(true);
        return LandingPageResponse.from(flushCheckingSlug(page));
    }

    // exclusão lógica: a página some da lista e da área pública, e o slug fica livre
    @Transactional
    public void delete(UUID tenantId, UUID pageId) {
        LandingPage page = lockPage(tenantId, pageId);
        page.setDeletedAt(OffsetDateTime.now());
        pageRepository.flush();
    }

    private LandingPage lockPage(UUID tenantId, UUID pageId) {
        return pageRepository.findForUpdate(pageId, tenantId)
                .orElseThrow(() -> new PageNotFoundException(PAGE_NOT_FOUND_KEY));
    }

    LandingPage findPage(UUID tenantId, UUID pageId) {
        return pageRepository.findByIdAndTenantIdAndDeletedAtIsNull(pageId, tenantId)
                .orElseThrow(() -> new PageNotFoundException(PAGE_NOT_FOUND_KEY));
    }

    private void applySettings(LandingPage page, LandingPageRequest request) {
        page.setName(request.nome().trim());
        page.setSlug(request.slug());
        page.setSeo(toSeoMap(request.seo()));
        page.setTracking(request.rastreamento() == null ? null : new LinkedHashMap<>(request.rastreamento()));
        page.setTheme(toThemeMap(request.tema()));
    }

    // a checagem de slug antes de salvar não cobre duas requisições simultâneas; nesse caso
    // quem recusa é o índice único do banco, e a resposta continua sendo 409
    private LandingPage flushCheckingSlug(LandingPage page) {
        try {
            pageRepository.flush();
            return page;
        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains(SLUG_UNIQUE_INDEX)) {
                throw new SlugAlreadyInUseException(page.getSlug());
            }
            throw ex;
        }
    }

    private static Map<String, Object> toSeoMap(LandingPageRequest.Seo seo) {
        if (seo == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("titulo", seo.titulo());
        map.put("descricao", seo.descricao());
        map.put("imagemCompartilhamento", seo.imagemCompartilhamento());
        map.put("indexar", seo.indexar() == null || seo.indexar());
        return map;
    }

    private static Map<String, Object> toThemeMap(LandingPageRequest.Tema tema) {
        if (tema == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("corPrimaria", tema.corPrimaria());
        map.put("corFundo", tema.corFundo());
        map.put("fonte", tema.fonte());
        return map;
    }

    // ?ordenar=campo,direcao (ex.: criadoEm,desc); sem direção, ordena crescente
    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }
        String[] parts = sort.split(",");
        String property = SORTABLE_FIELDS.get(parts[0].trim());
        if (property == null || parts.length > 2) {
            throw new InvalidQueryParameterException("ordenar: " + sort);
        }
        if (parts.length == 1) {
            return Sort.by(Sort.Direction.ASC, property);
        }
        Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim())
                .orElseThrow(() -> new InvalidQueryParameterException("ordenar: " + sort));
        return Sort.by(direction, property);
    }
}
