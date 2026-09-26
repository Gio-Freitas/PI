package com.grupo.formslp.omnipages.chatbot.dto;

import com.grupo.formslp.omnipages.chatbot.domain.ChatbotSessionStatus;

import java.util.List;

public record ChatAnswerResponse(ChatbotSessionStatus status, List<ChatMessageResponse> messages) {
}
