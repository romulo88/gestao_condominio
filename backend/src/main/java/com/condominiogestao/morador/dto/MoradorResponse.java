package com.condominiogestao.morador.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.morador.Morador;
import java.time.LocalDateTime;

/** Nunca expõe {@code senhaHash}. */
public record MoradorResponse(
        Integer id,
        String nome,
        String email,
        String telefone,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MoradorResponse from(Morador morador) {
        return new MoradorResponse(
                morador.getId(),
                morador.getNome(),
                morador.getEmail(),
                morador.getTelefone(),
                morador.getSituacao(),
                morador.getCreatedAt(),
                morador.getUpdatedAt());
    }
}
