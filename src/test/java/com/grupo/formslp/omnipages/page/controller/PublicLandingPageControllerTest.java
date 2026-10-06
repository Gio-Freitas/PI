package com.grupo.formslp.omnipages.page.controller;

import com.grupo.formslp.omnipages.page.dto.PublishedPageResponse;
import com.grupo.formslp.omnipages.page.exception.PageExceptionHandler;
import com.grupo.formslp.omnipages.page.exception.PageNotFoundException;
import com.grupo.formslp.omnipages.page.service.LandingPagePublicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// a área pública não pede tenant nem token e devolve só o que o contrato chama de PaginaPublicada
@WebMvcTest(PublicLandingPageController.class)
@Import(PageExceptionHandler.class)
class PublicLandingPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LandingPagePublicationService publicationService;

    @Test
    void publishedPageNeedsNoTenantAndExposesOnlyThePublicFields() throws Exception {
        when(publicationService.getPublished("black-friday")).thenReturn(new PublishedPageResponse("black-friday",
                Map.of("titulo", "Ofertas"), "<!DOCTYPE html>", List.of("contato"), OffsetDateTime.now()));

        mockMvc.perform(get("/public/landing/paginas/black-friday"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.html").value("<!DOCTYPE html>"))
                .andExpect(jsonPath("$.data.formularios[0]").value("contato"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist());
    }

    @Test
    void pageOutOfTheAirIs404() throws Exception {
        when(publicationService.getPublished("rascunho"))
                .thenThrow(new PageNotFoundException("page.error.public-page-not-found"));

        mockMvc.perform(get("/public/landing/paginas/rascunho"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Página não encontrada."));
    }
}
