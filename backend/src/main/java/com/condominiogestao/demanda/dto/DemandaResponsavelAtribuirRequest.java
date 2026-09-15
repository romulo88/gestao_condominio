package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;

/** Atribui pelo CPF - mais simples pra tela do que pedir o funcionarioId (a combo de
 * busca já resolve nome/unidade pro CPF certo, ver {@code CandidatoResponsavelResponse}). */
public record DemandaResponsavelAtribuirRequest(@NotBlank(message = "cpf é obrigatório") String cpf) {
}
