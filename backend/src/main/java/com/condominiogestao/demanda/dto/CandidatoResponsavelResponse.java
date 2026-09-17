package com.condominiogestao.demanda.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/** Um funcionário ativo no condomínio da demanda - alimenta a combo de busca de "atribuir
 * responsável" (nome + cargo/função na sugestão - pedido do Romulo: perfil pra quem tem
 * login, {@code funcao} como alternativa pra quem não tem - {@code cpf} continua vindo só
 * pra identificar o candidato ao selecionar, não é mais exibido). Só funcionário (diferente
 * de {@code CandidatoAcessoResponse}, do item 4.8, que também aceita morador) - não faz
 * sentido morador ser responsável por uma demanda. */
public record CandidatoResponsavelResponse(String cpf, String nome, FuncionarioPerfil perfil, String funcao) {
}
