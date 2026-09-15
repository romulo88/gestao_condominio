package com.condominiogestao.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "cpf é obrigatório") String cpf,
        @NotBlank(message = "senha é obrigatória") String senha) {
}
