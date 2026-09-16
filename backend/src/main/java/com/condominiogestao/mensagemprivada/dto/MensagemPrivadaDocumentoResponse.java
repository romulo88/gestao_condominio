package com.condominiogestao.mensagemprivada.dto;

import com.condominiogestao.mensagemprivada.MensagemPrivadaDocumento;
import java.time.LocalDateTime;

/** {@code url} é o caminho relativo (mesma origem da API) que serve a foto - {@code GET
 * .../{id}/arquivo}, mesmo padrão de {@code DemandaDocumentoResponse}: bucket privado,
 * quem pede esse caminho passa de novo pela checagem de "é participante dessa conversa". */
public record MensagemPrivadaDocumentoResponse(
        Integer id, Integer mensagemId, String nomeArquivo, String url, String tipoMime, Integer tamanhoBytes, LocalDateTime createdAt) {

    public static MensagemPrivadaDocumentoResponse from(MensagemPrivadaDocumento documento, String caminhoArquivo) {
        return new MensagemPrivadaDocumentoResponse(
                documento.getId(),
                documento.getMensagem().getId(),
                documento.getNomeArquivo(),
                caminhoArquivo,
                documento.getTipoMime(),
                documento.getTamanhoBytes(),
                documento.getCreatedAt());
    }
}
