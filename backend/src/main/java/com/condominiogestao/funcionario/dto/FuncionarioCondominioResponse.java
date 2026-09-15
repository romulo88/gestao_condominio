package com.condominiogestao.funcionario.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.funcionario.FuncionarioCondominio;
import com.condominiogestao.funcionario.FuncionarioPerfil;
import java.time.LocalDateTime;

public record FuncionarioCondominioResponse(
        Integer id,
        Integer funcionarioId,
        Integer condominioId,
        FuncionarioPerfil perfil,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static FuncionarioCondominioResponse from(FuncionarioCondominio vinculo) {
        return new FuncionarioCondominioResponse(
                vinculo.getId(),
                vinculo.getFuncionario().getId(),
                vinculo.getCondominio().getId(),
                vinculo.getPerfil(),
                vinculo.getSituacao(),
                vinculo.getCreatedAt(),
                vinculo.getUpdatedAt());
    }
}
