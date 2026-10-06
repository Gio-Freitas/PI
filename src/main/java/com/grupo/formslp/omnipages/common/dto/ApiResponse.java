package com.grupo.formslp.omnipages.common.dto;

import java.util.List;

// Envelope padrão dos oito módulos (Contrato de Integração §8.2)
public record ApiResponse<T>(boolean success, T data, String message, List<ApiError> errors) {

    public ApiResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, List.of());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, List.of());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, List.of());
    }

    public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, null, message, errors);
    }

    public static <T> ApiResponse<T> error(String message, String campo, String codigo, String detalhe) {
        return error(message, List.of(new ApiError(campo, codigo, detalhe)));
    }
}