package com.condominiogestao.mensagemprivada.dto;

import com.condominiogestao.mensagemprivada.MensagemPrivada;
import java.time.LocalDateTime;
import java.util.List;

/** Uma linha do chat. {@code autorTipo} é {@code "morador"} ou {@code "funcionario"}.
 * {@code minha} é calculado por quem está vendo (mesmo padrão de {@code
 * DemandaNotaResponse.podeResponder}) - o frontend usa isso pra alinhar o balão à direita/
 * esquerda, sem precisar comparar nome (dois funcionários podem ter o mesmo nome). */
public record MensagemPrivadaResponse(
        Integer id,
        Integer conversaId,
        String autorTipo,
        String autorNome,
        String texto,
        boolean minha,
        List<MensagemPrivadaDocumentoResponse> anexos,
        LocalDateTime createdAt) {

    public static MensagemPrivadaResponse from(
            MensagemPrivada mensagem, boolean minha, List<MensagemPrivadaDocumentoResponse> anexos) {
        boolean autorMorador = mensagem.getMoradorAutor() != null;
        return new MensagemPrivadaResponse(
                mensagem.getId(),
                mensagem.getConversa().getId(),
                autorMorador ? "morador" : "funcionario",
                autorMorador ? mensagem.getMoradorAutor().getNome() : mensagem.getFuncionarioAutor().getNome(),
                mensagem.getTexto(),
                minha,
                anexos,
                mensagem.getCreatedAt());
    }
}
