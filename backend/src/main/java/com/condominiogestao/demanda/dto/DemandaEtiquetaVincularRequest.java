package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotNull;

public record DemandaEtiquetaVincularRequest(
        @NotNull(message = "demandaId é obrigatório") Integer demandaId,
        @NotNull(message = "etiquetaId é obrigatório") Integer etiquetaId) {
}
