package com.grupo.formslp.omnipages.page.controller;

import com.grupo.formslp.omnipages.page.dto.BlockRequest;
import com.grupo.formslp.omnipages.page.dto.BlockResponse;
import com.grupo.formslp.omnipages.page.enums.BlockType;
import com.grupo.formslp.omnipages.page.exception.PageExceptionHandler;
import com.grupo.formslp.omnipages.page.service.LandingPageBlockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// rotas e nomes do contrato para os blocos: tipos em português na entrada e na saída, /ordem e /duplicar
@WebMvcTest(LandingPageBlockController.class)
@Import(PageExceptionHandler.class)
class LandingPageBlockControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PAGE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID BLOCK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final String BLOCKS = "/api/landing/paginas/" + PAGE_ID + "/blocos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LandingPageBlockService blockService;

    @Test
    void addReadsAndWritesContractTypeNames() throws Exception {
        BlockRequest expected = new BlockRequest(BlockType.HERO, Map.of("titulo", "Até 40%"), null);
        when(blockService.add(TENANT_ID, PAGE_ID, expected))
                .thenReturn(new BlockResponse(BLOCK_ID, 0, BlockType.HERO, Map.of("titulo", "Até 40%"), null));

        mockMvc.perform(post(BLOCKS).header("X-Tenant-Id", TENANT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"TITULO","conteudo":{"titulo":"Até 40%"}}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BLOCKS + "/" + BLOCK_ID))
                .andExpect(jsonPath("$.data.tipo").value("TITULO"))
                .andExpect(jsonPath("$.data.ordem").value(0));
    }

    @Test
    void internalTypeNamesAreNotAccepted() throws Exception {
        mockMvc.perform(post(BLOCKS).header("X-Tenant-Id", TENANT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"HERO","conteudo":{}}"""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(blockService);
    }

    @Test
    void orderRouteIsNotMistakenForABlockId() throws Exception {
        UUID other = UUID.randomUUID();

        mockMvc.perform(put(BLOCKS + "/ordem").header("X-Tenant-Id", TENANT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + BLOCK_ID + "\",\"" + other + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        verify(blockService).reorder(eq(TENANT_ID), eq(PAGE_ID), eq(List.of(BLOCK_ID, other)));
    }

    @Test
    void duplicateAnswers201WithTheCopyLocation() throws Exception {
        UUID copyId = UUID.randomUUID();
        when(blockService.duplicate(TENANT_ID, PAGE_ID, BLOCK_ID))
                .thenReturn(new BlockResponse(copyId, 1, BlockType.TEXT, Map.of(), null));

        mockMvc.perform(post(BLOCKS + "/" + BLOCK_ID + "/duplicar").header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BLOCKS + "/" + copyId));
    }
}
