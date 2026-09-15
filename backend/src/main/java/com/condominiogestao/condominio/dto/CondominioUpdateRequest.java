package com.condominiogestao.condominio.dto;

import com.condominiogestao.condominio.CondominioTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CondominioUpdateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "cnpj é obrigatório") String cnpj,
        @NotNull(message = "tipo é obrigatório") CondominioTipo tipo,
        Integer quantidadeCasas) {
}
