package com.condominiogestao.mensagemrapida.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.mensagemrapida.MensagemRapida;
import com.condominiogestao.mensagemrapida.MensagemRapidaCarater;
import java.time.LocalDateTime;

public record MensagemRapidaResponse(
        Integer id,
        Integer condominioId,
        String texto,
        MensagemRapidaCarater carater,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MensagemRapidaResponse from(MensagemRapida mensagem) {
        return new MensagemRapidaResponse(
                mensagem.getId(),
                mensagem.getCondominio().getId(),
                mensagem.getTexto(),
                mensagem.getCarater(),
                mensagem.getSituacao(),
                mensagem.getCreatedAt(),
                mensagem.getUpdatedAt());
    }
}
