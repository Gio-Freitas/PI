package com.grupo.formslp.omnipages.page.controller;

import com.grupo.formslp.omnipages.common.dto.ApiResponse;
import com.grupo.formslp.omnipages.page.dto.BlockOrderRequest;
import com.grupo.formslp.omnipages.page.dto.BlockRequest;
import com.grupo.formslp.omnipages.page.dto.BlockResponse;
import com.grupo.formslp.omnipages.page.service.LandingPageBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.grupo.formslp.omnipages.page.controller.LandingPageController.TENANT_HEADER;

// Blocos do rascunho de uma landing page (tela 2), rotas de contratos/landing.yaml
// PROVISÓRIO: o tenant vem do cabeçalho X-Tenant-Id, como no LandingPageController
@RestController
@RequestMapping("/api/landing/paginas/{pageId}/blocos")
@RequiredArgsConstructor
public class LandingPageBlockController {

    private final LandingPageBlockService blockService;
    private final MessageSource messageSource;

    @GetMapping
    public ApiResponse<List<BlockResponse>> list(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID pageId) {
        return ApiResponse.ok(blockService.list(tenantId, pageId), null);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlockResponse>> add(@RequestHeader(TENANT_HEADER) UUID tenantId,
                                                          @PathVariable UUID pageId,
                                                          @Valid @RequestBody BlockRequest request) {
        return created(pageId, blockService.add(tenantId, pageId, request), "page.response.block-created");
    }

    // declarado antes de /{blockId} só por leitura; o Spring escolhe a rota literal de qualquer forma
    @PutMapping("/ordem")
    public ApiResponse<Void> reorder(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID pageId,
                                     @Valid @RequestBody BlockOrderRequest request) {
        blockService.reorder(tenantId, pageId, request.ids());
        return ApiResponse.ok(null, i18n("page.response.blocks-reordered"));
    }

    @PutMapping("/{blockId}")
    public ApiResponse<BlockResponse> update(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID pageId,
                                             @PathVariable UUID blockId, @Valid @RequestBody BlockRequest request) {
        return ApiResponse.ok(blockService.update(tenantId, pageId, blockId, request), i18n("page.response.block-updated"));
    }

    @DeleteMapping("/{blockId}")
    public ApiResponse<Void> delete(@RequestHeader(TENANT_HEADER) UUID tenantId, @PathVariable UUID pageId,
                                    @PathVariable UUID blockId) {
        blockService.delete(tenantId, pageId, blockId);
        return ApiResponse.ok(null, i18n("page.response.block-deleted"));
    }

    @PostMapping("/{blockId}/duplicar")
    public ResponseEntity<ApiResponse<BlockResponse>> duplicate(@RequestHeader(TENANT_HEADER) UUID tenantId,
                                                                @PathVariable UUID pageId, @PathVariable UUID blockId) {
        return created(pageId, blockService.duplicate(tenantId, pageId, blockId), "page.response.block-duplicated");
    }

    private ResponseEntity<ApiResponse<BlockResponse>> created(UUID pageId, BlockResponse block, String messageKey) {
        return ResponseEntity.created(URI.create("/api/landing/paginas/" + pageId + "/blocos/" + block.id()))
                .body(ApiResponse.ok(block, i18n(messageKey)));
    }

    private String i18n(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
