package com.grupo.formslp.omnipages.chatbot.exception;

import com.grupo.formslp.omnipages.chatbot.controller.PublicChatController;
import com.grupo.formslp.omnipages.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice(assignableTypes = PublicChatController.class)
@RequiredArgsConstructor
public class ChatbotExceptionHandler {

    private static final String INVALID_REQUEST_KEY = "chatbot.error.invalid-request";

    private final MessageSource messageSource;

    @ExceptionHandler(ChatbotNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(ChatbotNotFoundException ex) {
        return ApiResponse.error(i18n(ex.getMessageKey()), List.of());
    }

    @ExceptionHandler(ChatbotBusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse<Void> businessRule(ChatbotBusinessException ex) {
        return ApiResponse.error(i18n(ex.getMessageKey()), List.of());
    }

    // a mensagem de cada campo já vem traduzida pelo Bean Validation, que usa o mesmo messages.properties
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidBody(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ApiResponse.error(i18n(INVALID_REQUEST_KEY), errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> malformedRequest(Exception ex) {
        return ApiResponse.error(i18n(INVALID_REQUEST_KEY), List.of());
    }

    private String i18n(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
