package com.condominiogestao.espacocomum.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.espacocomum.EspacoComum;
import java.time.LocalDateTime;

public record EspacoComumResponse(
        Integer id, Integer condominioId, String nome, Situacao situacao, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static EspacoComumResponse from(EspacoComum espaco) {
        return new EspacoComumResponse(
                espaco.getId(),
                espaco.getCondominio().getId(),
                espaco.getNome(),
                espaco.getSituacao(),
                espaco.getCreatedAt(),
                espaco.getUpdatedAt());
    }
}
