package com.condominiogestao.etiqueta.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.etiqueta.Etiqueta;
import java.time.LocalDateTime;

public record EtiquetaResponse(
        Integer id,
        Integer condominioId,
        String descricao,
        String cor,
        Situacao situacao,
        /** Controla se a etiqueta aparece pro morador (nos cards/detalhe da demanda) -
         * funcionário sempre vê/gerencia, independente deste valor. */
        boolean visivelMorador,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static EtiquetaResponse from(Etiqueta etiqueta) {
        return new EtiquetaResponse(
                etiqueta.getId(),
                etiqueta.getCondominio().getId(),
                etiqueta.getDescricao(),
                etiqueta.getCor(),
                etiqueta.getSituacao(),
                etiqueta.isVisivelMorador(),
                etiqueta.getCreatedAt(),
                etiqueta.getUpdatedAt());
    }
}
