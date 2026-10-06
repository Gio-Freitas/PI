package com.grupo.formslp.omnipages.page.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Optional;

// no banco fica em inglês, como o FormStatus; na API sai com o nome do contrato (contratos/landing.yaml)
public enum LandingPageStatus {
    @JsonProperty("RASCUNHO") DRAFT("RASCUNHO"),
    @JsonProperty("PUBLICADA") PUBLISHED("PUBLICADA"),
    @JsonProperty("ARQUIVADA") ARCHIVED("ARQUIVADA");

    private final String contractName;

    LandingPageStatus(String contractName) {
        this.contractName = contractName;
    }

    // usado no filtro ?status= da listagem, que chega como texto
    public static Optional<LandingPageStatus> fromContractName(String value) {
        return Arrays.stream(values()).filter(s -> s.contractName.equalsIgnoreCase(value)).findFirst();
    }
}
