package com.grupo.formslp.omnipages.chatbot.dto;

import java.util.List;

// Envelope padrão da plataforma; quando outros módulos precisarem dele, mover para um pacote common
public record ApiResponse<T>(boolean success, T data, String message, List<String> errors) {

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, List.of());
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, null, message, errors);
    }
}
