package com.condominiogestao.demanda.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/** Um funcionário ativo no condomínio da demanda - alimenta a combo de busca de "atribuir
 * responsável" (nome + cargo/função na sugestão - pedido do Romulo: perfil pra quem tem
 * login, {@code funcao} como alternativa pra quem não tem). {@code funcionarioId} é o
 * identificador enviado de volta ao selecionar (CPF saiu do sistema, v177 - LGPD). Só
 * funcionário (diferente de {@code CandidatoAcessoResponse}, do item 4.8, que também
 * aceita morador) - não faz sentido morador ser responsável por uma demanda. */
public record CandidatoResponsavelResponse(Integer funcionarioId, String nome, FuncionarioPerfil perfil, String funcao) {
}
