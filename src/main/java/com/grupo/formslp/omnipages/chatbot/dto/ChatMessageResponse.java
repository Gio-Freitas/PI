package com.grupo.formslp.omnipages.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grupo.formslp.omnipages.chatbot.domain.ChatbotMessage;
import com.grupo.formslp.omnipages.chatbot.domain.ChatbotMessageRole;

// fieldName só vem preenchido quando a mensagem é uma pergunta, para o cliente adaptar a entrada (ex.: teclado numérico)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(ChatbotMessageRole role, String content, String fieldName) {

    public static ChatMessageResponse from(ChatbotMessage message) {
        return new ChatMessageResponse(message.getRole(), message.getContent(), message.getFieldName());
    }
}
