package com.condominiogestao.funcionario.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;
import jakarta.validation.constraints.NotNull;

/** {@code perfil} nulo = funcionário sem acesso ao sistema nesse condomínio (item 2.1).
 * {@code funcao} é texto livre (jardineiro, rondista, etc.) - pedido do Romulo pra
 * identificar quem não tem perfil, mas não é exigido nem restrito a esse caso. */
public record FuncionarioCondominioCreateRequest(
        @NotNull(message = "funcionarioId é obrigatório") Integer funcionarioId,
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        FuncionarioPerfil perfil,
        String funcao) {
}
