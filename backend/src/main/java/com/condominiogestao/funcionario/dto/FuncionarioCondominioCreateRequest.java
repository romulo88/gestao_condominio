package com.condominiogestao.funcionario.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;
import jakarta.validation.constraints.NotNull;

/** {@code perfil} nulo = funcionário sem acesso ao sistema nesse condomínio (item 2.1). */
public record FuncionarioCondominioCreateRequest(
        @NotNull(message = "funcionarioId é obrigatório") Integer funcionarioId,
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        FuncionarioPerfil perfil) {
}
