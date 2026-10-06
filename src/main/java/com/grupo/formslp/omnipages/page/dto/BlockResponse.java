package com.grupo.formslp.omnipages.page.dto;

import com.grupo.formslp.omnipages.page.entity.LandingPageBlock;
import com.grupo.formslp.omnipages.page.enums.BlockType;

import java.util.Map;
import java.util.UUID;

// bloco do rascunho, como o editor (tela 2) recebe; segue o Bloco do contrato
public record BlockResponse(
        UUID id,
        Integer ordem,
        BlockType tipo,
        Map<String, Object> conteudo,
        UUID formularioId
) {

    public static BlockResponse from(LandingPageBlock block) {
        return new BlockResponse(
                block.getId(),
                block.getPosition(),
                block.getType(),
                block.getContent(),
                block.getForm() == null ? null : block.getForm().getId()
        );
    }
}
