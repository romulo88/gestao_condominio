package com.condominiogestao.espacocomum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EspacoComumCreateRequest(
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 50, message = "nome deve ter no máximo 50 caracteres")
                String nome) {
}
