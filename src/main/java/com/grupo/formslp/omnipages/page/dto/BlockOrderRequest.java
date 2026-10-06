package com.grupo.formslp.omnipages.page.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// nova ordem dos blocos: todos os ids da página, do primeiro ao último (Reordenacao no contrato)
public record BlockOrderRequest(@NotNull List<UUID> ids) {
}
