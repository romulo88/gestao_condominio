package com.condominiogestao.mensagemprivada.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/** Um funcionário COM LOGIN (perfil preenchido) do condomínio - alimenta a combo de busca
 * de destinatário (por NOME - pedido do Romulo: "vai ser difícil saber o cpf do
 * funcionário") na hora de criar uma mensagem privada. {@code funcionarioId} é o
 * identificador que o front manda de volta em {@code ConversaPrivadaCreateRequest} (CPF
 * saiu do sistema, v177/LGPD) - {@code perfil} entra na sugestão, informação bem mais útil
 * pra identificar quem é quem. Diferente de {@code CandidatoResponsavelResponse}
 * (demanda): ali qualquer funcionário ativo serve, aqui só quem tem login de verdade -
 * endereçar a quem não loga no sistema não faz sentido. */
public record CandidatoDestinatarioResponse(Integer funcionarioId, String nome, FuncionarioPerfil perfil) {
}
