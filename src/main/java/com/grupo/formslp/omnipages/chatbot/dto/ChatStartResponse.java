package com.grupo.formslp.omnipages.chatbot.dto;

import java.util.List;
import java.util.UUID;

public record ChatStartResponse(UUID sessionId, List<ChatMessageResponse> messages) {
}
