package com.grupo.formslp.omnipages.page.exception;

import lombok.Getter;

// página ou bloco inexistente, de outro tenant ou excluído -> 404; o texto é resolvido pela chave no messages.properties
@Getter
public class PageNotFoundException extends RuntimeException {
    private final String messageKey;

    public PageNotFoundException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
