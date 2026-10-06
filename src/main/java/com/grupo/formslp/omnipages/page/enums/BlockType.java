package com.grupo.formslp.omnipages.page.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

// no banco fica em inglês; na API sai com o nome do contrato (contratos/landing.yaml)
public enum BlockType {
    @JsonProperty("TITULO") HERO,
    @JsonProperty("TEXTO") TEXT,
    @JsonProperty("IMAGEM") IMAGE,
    @JsonProperty("BENEFICIOS") BENEFITS,
    @JsonProperty("DEPOIMENTOS") TESTIMONIALS,
    @JsonProperty("FORMULARIO") FORM,
    @JsonProperty("CHAMADA_ACAO") CTA,
    @JsonProperty("PERGUNTAS_FREQUENTES") FAQ,
    @JsonProperty("RODAPE") FOOTER
}
