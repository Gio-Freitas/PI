package com.grupo.formslp.omnipages.common.dto;

public record ApiError(String campo, String codigo, String detalhe) {
    public static ApiError geral(String codigo, String detalhe) {
        return new ApiError(null, codigo, detalhe);
    }
}
