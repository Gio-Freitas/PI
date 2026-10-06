package com.grupo.formslp.omnipages.page.dto;

import com.grupo.formslp.omnipages.page.enums.BlockType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

// corpo de criação e de edição de bloco (tela 2); segue o BlocoEntrada do contrato
// o conteúdo é livre por tipo: quem decide o que cada chave significa é a geração do HTML na publicação
public record BlockRequest(
        @NotNull BlockType tipo,
        @NotNull Map<String, Object> conteudo,
        UUID formularioId
) {
}
