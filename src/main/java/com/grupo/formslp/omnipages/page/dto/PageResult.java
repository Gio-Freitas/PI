package com.grupo.formslp.omnipages.page.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// resultado paginado no formato do contrato (PaginaResultado): itens, pagina, tamanho e total
public record PageResult<T>(List<T> itens, int pagina, int tamanho, long total) {

    public static <E, T> PageResult<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
