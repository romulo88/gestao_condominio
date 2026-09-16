package com.condominiogestao.mensagemprivada.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/** Um funcionário COM LOGIN (perfil preenchido) do condomínio - alimenta a combo de busca
 * de destinatário (por NOME - pedido do Romulo: "vai ser difícil saber o cpf do
 * funcionário") na hora de criar uma mensagem privada. {@code cpf} continua vindo (é o
 * identificador que o front manda de volta em {@code ConversaPrivadaCreateRequest}), só não
 * é mais exibido - {@code perfil} entra no lugar do CPF na sugestão, informação bem mais
 * útil pra identificar quem é quem. Diferente de {@code CandidatoResponsavelResponse}
 * (demanda): ali qualquer funcionário ativo serve, aqui só quem tem login de verdade -
 * endereçar a quem não loga no sistema não faz sentido. */
public record CandidatoDestinatarioResponse(String cpf, String nome, FuncionarioPerfil perfil) {
}
