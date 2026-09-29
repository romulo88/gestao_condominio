package com.condominiogestao.evento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventoVeiculoRequest(
        @NotBlank(message = "placa é obrigatória") @Size(max = 8, message = "placa deve ter no máximo 8 caracteres")
                String placa) {
}
