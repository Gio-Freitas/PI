package com.grupo.formslp.omnipages.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatAnswerRequest(@NotBlank(message = "{chatbot.validation.answer-blank}") String answer) {
}
