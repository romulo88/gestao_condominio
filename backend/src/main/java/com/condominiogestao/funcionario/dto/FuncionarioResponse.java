package com.condominiogestao.funcionario.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.funcionario.Funcionario;
import java.time.LocalDateTime;

/** Nunca expõe {@code senhaHash}. */
public record FuncionarioResponse(
        Integer id,
        String nome,
        String email,
        String telefone,
        /** Link assinado (expira em 15min) - null quando a pessoa não tem foto
         * cadastrada. Ver {@code PessoaFotoService}. */
        String fotoUrl,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static FuncionarioResponse from(Funcionario funcionario, String fotoUrl) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getEmail(),
                funcionario.getTelefone(),
                fotoUrl,
                funcionario.getSituacao(),
                funcionario.getCreatedAt(),
                funcionario.getUpdatedAt());
    }
}
