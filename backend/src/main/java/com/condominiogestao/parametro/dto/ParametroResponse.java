package com.condominiogestao.parametro.dto;

import com.condominiogestao.parametro.Parametro;
import java.time.LocalDateTime;

public record ParametroResponse(
        Integer id,
        String nome,
        String descricao,
        String valor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ParametroResponse from(Parametro parametro) {
        return new ParametroResponse(
                parametro.getId(),
                parametro.getNome(),
                parametro.getDescricao(),
                parametro.getValor(),
                parametro.getCreatedAt(),
                parametro.getUpdatedAt());
    }
}
