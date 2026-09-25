package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotNull;

/** Concede acesso pelo id da Pessoa (mesma PK de {@code Morador}/{@code Funcionario} -
 * CPF saiu do sistema, v177/LGPD) - a mesma pessoa pode ter os dois papéis; o service
 * concede em todos os que ela tiver vínculo ativo com o condomínio da demanda. */
public record DemandaAcessoSigilosoConcederRequest(@NotNull(message = "pessoaId é obrigatório") Integer pessoaId) {
}
