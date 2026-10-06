package com.grupo.formslp.omnipages.page.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

// corpo de criação e de edição da página (telas 1 e 3); nomes dos campos seguem o LandingPageEntrada do contrato
// os formatos são fechados de propósito: tema, SEO e rastreamento vão parar dentro do HTML publicado
public record LandingPageRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "{page.validation.slug-format}")
        String slug,
        @Valid Seo seo,
        @Size(max = 20)
        Map<@NotBlank @Size(max = 50) String, @NotBlank @Size(max = 200) String> rastreamento,
        @Valid Tema tema
) {

    public record Seo(
            @Size(max = 70) String titulo,
            @Size(max = 160) String descricao,
            @Size(max = 2048)
            @Pattern(regexp = "^https?://\\S+$", message = "{page.validation.url-format}")
            String imagemCompartilhamento,
            Boolean indexar
    ) {
    }

    public record Tema(
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{page.validation.color-format}") String corPrimaria,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{page.validation.color-format}") String corFundo,
            @Size(max = 60)
            @Pattern(regexp = "^[A-Za-z0-9 \\-]+$", message = "{page.validation.font-format}")
            String fonte
    ) {
    }
}
