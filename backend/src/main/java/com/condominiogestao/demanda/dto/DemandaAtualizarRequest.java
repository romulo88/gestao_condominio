package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;

/** Edição pelo próprio solicitante enquanto a demanda está pendente (pedido do Romulo) -
 * só título e descrição; sigilo, identificação do solicitante e ronda não mudam depois de
 * criada. Ver {@code DemandaService#atualizar}. */
public record DemandaAtualizarRequest(
        @NotBlank(message = "titulo é obrigatório") String titulo,
        @NotBlank(message = "descricao é obrigatória") String descricao) {
}
