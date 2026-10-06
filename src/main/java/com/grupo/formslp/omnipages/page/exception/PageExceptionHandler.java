package com.grupo.formslp.omnipages.page.exception;

import com.grupo.formslp.omnipages.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

// vale só para os controllers de landing pages, como o ChatbotExceptionHandler faz com o chatbot
@RestControllerAdvice(basePackages = "com.grupo.formslp.omnipages.page")
@RequiredArgsConstructor
public class PageExceptionHandler {

    private static final String INVALID_REQUEST_KEY = "page.error.invalid-request";

    private final MessageSource messageSource;

    @ExceptionHandler(PageNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(PageNotFoundException ex) {
        return ApiResponse.error(i18n(ex.getMessageKey()), List.of());
    }

    @ExceptionHandler(SlugAlreadyInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> slugInUse(SlugAlreadyInUseException ex) {
        return ApiResponse.error(i18n("page.error.slug-in-use"), List.of("slug: " + i18n("page.error.slug-in-use.detail")));
    }

    // sem o cabeçalho do tenant não há usuário identificado (NaoAutenticado no contrato);
    // some junto com o X-Tenant-Id quando o token JWT entrar
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> unauthenticated(MissingRequestHeaderException ex) {
        return ApiResponse.error(i18n("page.error.unauthenticated"), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidBody(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ApiResponse.error(i18n(INVALID_REQUEST_KEY), errors);
    }

    @ExceptionHandler(PageStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse<Void> invalidState(PageStateException ex) {
        return ApiResponse.error(i18n(ex.getMessageKey()), List.of(ex.getCode()));
    }

    @ExceptionHandler(BlockValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidBlock(BlockValidationException ex) {
        return ApiResponse.error(i18n(INVALID_REQUEST_KEY), List.of(ex.getField() + ": " + i18n(ex.getMessageKey())));
    }

    @ExceptionHandler(InvalidQueryParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidQuery(InvalidQueryParameterException ex) {
        return ApiResponse.error(i18n(INVALID_REQUEST_KEY), List.of(ex.getMessage()));
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
