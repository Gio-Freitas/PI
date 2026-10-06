package com.grupo.formslp.omnipages.page.controller;

import com.grupo.formslp.omnipages.page.dto.LandingPageRequest;
import com.grupo.formslp.omnipages.page.dto.LandingPageResponse;
import com.grupo.formslp.omnipages.page.enums.LandingPageStatus;
import com.grupo.formslp.omnipages.page.exception.PageExceptionHandler;
import com.grupo.formslp.omnipages.page.exception.SlugAlreadyInUseException;
import com.grupo.formslp.omnipages.page.service.LandingPageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// trava o formato que o contrato (contratos/landing.yaml) promete: rotas, status HTTP, nomes em português e envelope
@WebMvcTest(LandingPageController.class)
@Import(PageExceptionHandler.class)
class LandingPageControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PAGE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final String VALID_BODY = """
            {"nome":"Black Friday","slug":"black-friday-2026","tema":{"corPrimaria":"#2563EB"}}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LandingPageService pageService;

    @Test
    void createReturns201WithLocationAndContractFieldNames() throws Exception {
        when(pageService.create(eq(TENANT_ID), any(LandingPageRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/landing/paginas").header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/landing/paginas/" + PAGE_ID))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.data.temAlteracaoNaoPublicada").value(true))
                .andExpect(jsonPath("$.data.blocos").isArray())
                .andExpect(jsonPath("$.message").value("Landing page criada."));
    }

    @Test
    void missingTenantIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/landing/paginas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty());
        verifyNoInteractions(pageService);
    }

    @Test
    void invalidSlugIsRejectedBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/landing/paginas").header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"X","slug":"Black Friday!","tema":{"fonte":"Inter;}body{display:none"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
        verifyNoInteractions(pageService);
    }

    @Test
    void duplicatedSlugIsConflict() throws Exception {
        when(pageService.create(eq(TENANT_ID), any(LandingPageRequest.class)))
                .thenThrow(new SlugAlreadyInUseException("black-friday-2026"));

        mockMvc.perform(post("/api/landing/paginas").header("X-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este endereço já está em uso."));
    }

    private static LandingPageResponse response() {
        return new LandingPageResponse(PAGE_ID, "Black Friday", "black-friday-2026", LandingPageStatus.DRAFT,
                null, null, null, null, true, List.of(), null, null, null, null);
    }
}
