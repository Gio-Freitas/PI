package com.grupo.formslp.omnipages.chatbot.exception;

// regra de negócio violada (formulário inativo, conversa já encerrada) -> 422
public class ChatbotBusinessException extends RuntimeException {
    public ChatbotBusinessException(String message) {
        super(message);
    }
}
