package com.grupo.formslp.omnipages.page.controller;

import com.grupo.formslp.omnipages.common.dto.ApiResponse;
import com.grupo.formslp.omnipages.page.dto.PublishedPageResponse;
import com.grupo.formslp.omnipages.page.service.LandingPagePublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// Área pública (acesso anon, sem token): só entrega a versão publicada, nunca dado administrativo.
// O slug identifica a página e o tenant; trocar o slug de uma página publicada muda o endereço na hora
@RestController
@RequestMapping("/public/landing/paginas")
@RequiredArgsConstructor
public class PublicLandingPageController {

    private final LandingPagePublicationService publicationService;

    @GetMapping("/{slug}")
    public ApiResponse<PublishedPageResponse> get(@PathVariable String slug) {
        return ApiResponse.ok(publicationService.getPublished(slug), null);
    }
}
