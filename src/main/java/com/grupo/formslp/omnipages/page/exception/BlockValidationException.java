package com.grupo.formslp.omnipages.page.exception;

import lombok.Getter;

// bloco bem formado, mas inválido para a página (formulário de outro tenant, ordem incompleta...) -> 400
// guarda o campo do erro e a chave da mensagem no messages.properties
@Getter
public class BlockValidationException extends RuntimeException {
    private final String field;
    private final String messageKey;

    public BlockValidationException(String field, String messageKey) {
        super(field + ": " + messageKey);
        this.field = field;
        this.messageKey = messageKey;
    }
}
