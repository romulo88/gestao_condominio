package com.condominiogestao.mensagemprivada.dto;

import com.condominiogestao.mensagemprivada.ConversaPrivadaDestinatario;

public record ConversaPrivadaDestinatarioResponse(Integer funcionarioId, String nome) {

    public static ConversaPrivadaDestinatarioResponse from(ConversaPrivadaDestinatario destinatario) {
        return new ConversaPrivadaDestinatarioResponse(
                destinatario.getFuncionario().getId(), destinatario.getFuncionario().getNome());
    }
}
