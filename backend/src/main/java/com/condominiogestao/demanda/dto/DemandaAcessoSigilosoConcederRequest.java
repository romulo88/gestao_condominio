package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;

/** Concede acesso pelo CPF - mais simples pra tela do que pedir moradorId/funcionarioId
 * (a mesma pessoa pode ter os dois papéis; o service concede em todos os que ela tiver
 * vínculo ativo com o condomínio da demanda). */
public record DemandaAcessoSigilosoConcederRequest(@NotBlank(message = "cpf é obrigatório") String cpf) {
}
