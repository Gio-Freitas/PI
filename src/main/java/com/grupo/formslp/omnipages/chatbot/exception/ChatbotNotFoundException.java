package com.grupo.formslp.omnipages.chatbot.exception;

// formulário ou sessão inexistente -> 404
public class ChatbotNotFoundException extends RuntimeException {
    public ChatbotNotFoundException(String message) {
        super(message);
    }
}
