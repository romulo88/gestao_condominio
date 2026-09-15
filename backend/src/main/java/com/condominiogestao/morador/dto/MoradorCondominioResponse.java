package com.condominiogestao.morador.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.morador.MoradorCondominio;
import java.time.LocalDateTime;

public record MoradorCondominioResponse(
        Integer id,
        Integer moradorId,
        Integer condominioId,
        Integer blocoId,
        String numeroUnidade,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MoradorCondominioResponse from(MoradorCondominio vinculo) {
        return new MoradorCondominioResponse(
                vinculo.getId(),
                vinculo.getMorador().getId(),
                vinculo.getCondominio().getId(),
                vinculo.getBloco() != null ? vinculo.getBloco().getId() : null,
                vinculo.getNumeroUnidade(),
                vinculo.getSituacao(),
                vinculo.getCreatedAt(),
                vinculo.getUpdatedAt());
    }
}
