package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotNull;

/** Atribui pelo id do funcionário (a combo de busca já resolve nome/cargo pro id certo,
 * ver {@code CandidatoResponsavelResponse}) - CPF saiu do sistema (v177, LGPD). */
public record DemandaResponsavelAtribuirRequest(@NotNull(message = "funcionarioId é obrigatório") Integer funcionarioId) {
}
