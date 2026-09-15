package com.condominiogestao.demanda.dto;

/**
 * Uma pessoa (morador ou funcionário) ativa no condomínio da demanda - alimenta a combo
 * de busca do "Gerenciar acesso" (item 4.8), pra escolher por nome/unidade/CPF em vez de
 * digitar o CPF de cabeça. {@code unidade} só vem preenchida pra morador (bloco + número,
 * ou só número quando o condomínio não tem bloco) - funcionário não mora lá.
 */
public record CandidatoAcessoResponse(String cpf, String nome, String tipoPessoa, String unidade) {}
