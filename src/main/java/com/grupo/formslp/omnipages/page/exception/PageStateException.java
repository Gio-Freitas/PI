package com.grupo.formslp.omnipages.page.exception;

import lombok.Getter;

// pedido bem formado, mas a página não está num estado que permita a ação (publicar arquivada,
// despublicar rascunho...) -> 422; guarda o código do contrato (PAGINA_ARQUIVADA...) e a chave da mensagem
@Getter
public class PageStateException extends RuntimeException {
    private final String code;
    private final String messageKey;

    public PageStateException(String code, String messageKey) {
        super(code);
        this.code = code;
        this.messageKey = messageKey;
    }
}
