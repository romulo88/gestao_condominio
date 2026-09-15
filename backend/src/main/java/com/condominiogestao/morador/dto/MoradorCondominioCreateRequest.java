package com.condominiogestao.morador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MoradorCondominioCreateRequest(
        @NotNull(message = "moradorId é obrigatório") Integer moradorId,
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        Integer blocoId,
        @NotBlank(message = "numeroUnidade é obrigatório") String numeroUnidade) {
}
