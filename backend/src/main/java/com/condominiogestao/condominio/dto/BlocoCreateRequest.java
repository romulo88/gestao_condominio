package com.condominiogestao.condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BlocoCreateRequest(
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        @NotBlank(message = "nome é obrigatório") String nome) {
}
