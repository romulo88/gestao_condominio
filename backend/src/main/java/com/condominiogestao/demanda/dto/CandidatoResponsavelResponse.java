package com.condominiogestao.demanda.dto;

/** Um funcionário ativo no condomínio da demanda - alimenta a combo de busca de "atribuir
 * responsável" (nome + CPF), em vez da pessoa precisar decorar o CPF de quem quer
 * atribuir. Só funcionário (diferente de {@code CandidatoAcessoResponse}, do item 4.8,
 * que também aceita morador) - não faz sentido morador ser responsável por uma demanda. */
public record CandidatoResponsavelResponse(String cpf, String nome) {
}
