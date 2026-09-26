package com.grupo.formslp.omnipages.chatbot.exception;

import lombok.Getter;

// formulário ou sessão inexistente -> 404; o texto é resolvido pela chave no messages.properties
@Getter
public class ChatbotNotFoundException extends RuntimeException {
    private final String messageKey;

    public ChatbotNotFoundException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }
}
