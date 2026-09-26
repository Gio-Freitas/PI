package com.grupo.formslp.omnipages.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatAnswerRequest(@NotBlank(message = "A resposta não pode ficar em branco.") String answer) {
}
