package com.condominiogestao.aviso.dto;

import com.condominiogestao.aviso.Aviso;
import com.condominiogestao.common.Situacao;
import java.time.LocalDateTime;

public record AvisoResponse(
        Integer id,
        Integer condominioId,
        Integer funcionarioId,
        String funcionarioNome,
        String descricao,
        Situacao situacao,
        LocalDateTime dataExpiracao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AvisoResponse from(Aviso aviso) {
        return new AvisoResponse(
                aviso.getId(),
                aviso.getCondominio().getId(),
                aviso.getFuncionario().getId(),
                aviso.getFuncionario().getNome(),
                aviso.getDescricao(),
                aviso.getSituacao(),
                aviso.getDataExpiracao(),
                aviso.getCreatedAt(),
                aviso.getUpdatedAt());
    }
}
