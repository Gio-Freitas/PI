package com.grupo.formslp.omnipages.page.controller;

import com.grupo.formslp.omnipages.common.dto.ApiResponse;
import com.grupo.formslp.omnipages.page.dto.LandingPageRequest;
import com.grupo.formslp.omnipages.page.dto.LandingPageResponse;
import com.grupo.formslp.omnipages.page.dto.LandingPageSummaryResponse;
import com.grupo.formslp.omnipages.page.dto.PageResult;
import com.grupo.formslp.omnipages.page.service.LandingPagePublicationService;
import com.grupo.formslp.omnipages.page.service.LandingPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

// Área administrativa das landing pages (telas 1, 2 e 3), rotas de contratos/landing.yaml
// PROVISÓRIO: o tenant vem do cabeçalho X-Tenant-Id até a validação do token JWT entrar no módulo;
// depois ele passa a sair do token e o cabeçalho deixa de existir
@RestController
@RequestMapping("/api/landing/paginas")
@RequiredArgsConstructor
public class LandingPageController {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final LandingPageService pageService;
    private final LandingPagePublicationService publicationService;
    private final MessageSource messageSource;

    @GetMapping
    public ApiResponse<PageResult<LandingPageSummaryResponse>> list(
            @RequestHeader(TENANT_HEADER) UUID tenantId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(required = false) String ordenar,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(pageService.list(tenantId, nome, status, pagina, tamanho, ordenar), null);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LandingPageResponse>> create(@RequestHeader(TENANT_HEADER) UUID tenantId,
                                                                   @Valid @RequestBody LandingPageRequest request) {
        LandingPageResponse page = pageService.create(tenantId, request);
        return ResponseEntity.created(URI.create("/api/landing/paginas/" + page.id()))
                .body(ApiResponse.ok(page, i18n("page.response.created")));
    }

    @GetMapping("/{id}")
    public ApiResponse<LandingPageResponse> get(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID id) {
        return ApiResponse.ok(pageService.get(tenantId, id), null);
    }

    @PutMapping("/{id}")
    public ApiResponse<LandingPageResponse> update(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID id,
                                                   @Valid @RequestBody LandingPageRequest request) {
        return ApiResponse.ok(pageService.update(tenantId, id, request), i18n("page.response.updated"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID id) {
        pageService.delete(tenantId, id);
        return ApiResponse.ok(null, i18n("page.response.deleted"));
    }

    @PostMapping("/{id}/publicar")
    public ApiResponse<LandingPageResponse> publish(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID id) {
        return ApiResponse.ok(publicationService.publish(tenantId, id), i18n("page.response.published"));
    }

    @PostMapping("/{id}/despublicar")
    public ApiResponse<LandingPageResponse> unpublish(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID id) {
        return ApiResponse.ok(publicationService.unpublish(tenantId, id), i18n("page.response.unpublished"));
    }

    @PostMapping("/{id}/arquivar")
    public ApiResponse<LandingPageResponse> archive(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID id) {
        return ApiResponse.ok(publicationService.archive(tenantId, id), i18n("page.response.archived"));
    }

    private String i18n(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
