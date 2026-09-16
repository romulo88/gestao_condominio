package com.condominiogestao.mensagemprivada.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversaPrivadaDetalheResponse(
        Integer id,
        String autorTipo,
        String autorNome,
        List<ConversaPrivadaDestinatarioResponse> destinatarios,
        List<MensagemPrivadaResponse> mensagens,
        LocalDateTime createdAt) {
}
