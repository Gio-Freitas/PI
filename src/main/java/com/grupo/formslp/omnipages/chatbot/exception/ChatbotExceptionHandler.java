package com.grupo.formslp.omnipages.chatbot.exception;

import com.grupo.formslp.omnipages.chatbot.controller.PublicChatController;
import com.grupo.formslp.omnipages.chatbot.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice(assignableTypes = PublicChatController.class)
public class ChatbotExceptionHandler {

    @ExceptionHandler(ChatbotNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(ChatbotNotFoundException ex) {
        return ApiResponse.error(ex.getMessage(), List.of());
    }

    @ExceptionHandler(ChatbotBusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse<Void> businessRule(ChatbotBusinessException ex) {
        return ApiResponse.error(ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidBody(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ApiResponse.error("Requisição inválida.", errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> malformedRequest(Exception ex) {
        return ApiResponse.error("Requisição inválida.", List.of());
    }
}
