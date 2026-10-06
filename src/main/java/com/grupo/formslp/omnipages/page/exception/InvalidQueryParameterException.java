package com.grupo.formslp.omnipages.page.exception;

// filtro ou ordenação da listagem com valor que o contrato não prevê (?status=, ?ordenar=) -> 400
public class InvalidQueryParameterException extends RuntimeException {

    public InvalidQueryParameterException(String detail) {
        super(detail);
    }
}
