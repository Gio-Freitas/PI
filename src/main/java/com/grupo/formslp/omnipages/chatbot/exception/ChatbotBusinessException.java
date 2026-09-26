package com.grupo.formslp.omnipages.chatbot.exception;

import lombok.Getter;

// regra de negócio violada (formulário inativo, conversa já encerrada) -> 422; o texto é resolvido pela chave no messages.properties
@Getter
public class ChatbotBusinessException extends RuntimeException {
    private final String messageKey;

    public ChatbotBusinessException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
