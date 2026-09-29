package com.condominiogestao.evento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventoPessoaRequest(
        @NotBlank(message = "nome é obrigatório") @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
                String nome,
        @NotBlank(message = "documento é obrigatório")
                @Size(max = 50, message = "documento deve ter no máximo 50 caracteres")
                String documento) {
}
