package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaDocumento;
import java.time.LocalDateTime;

/**
 * {@code url} é o caminho relativo (mesma origem da API) que serve a imagem direto -
 * {@code GET .../{id}/arquivo}, ver {@code DemandaDocumentoController} +
 * {@code ArquivoStorageService}. O bucket é privado de propósito: quem pede esse caminho
 * ainda passa pela mesma checagem de visibilidade da demanda (sigilo incluso) de novo no
 * servidor - o caminho em si não é um "link mágico" que basta ter pra ver o arquivo, como
 * era o link assinado do MinIO antes (trocado porque não é alcançável de fora do servidor
 * via túnel/celular - ver HANDOFF.md).
 */
public record DemandaDocumentoResponse(
        Integer id,
        Integer demandaId,
        String nomeArquivo,
        String url,
        String tipoMime,
        Integer tamanhoBytes,
        String enviadoPorNome,
        LocalDateTime createdAt) {

    public static DemandaDocumentoResponse from(DemandaDocumento documento, String caminhoArquivo) {
        String enviadoPorNome = documento.getFuncionarioUpload() != null
                ? documento.getFuncionarioUpload().getNome()
                : documento.getMoradorUpload() != null ? documento.getMoradorUpload().getNome() : null;
        return new DemandaDocumentoResponse(
                documento.getId(),
                documento.getDemanda().getId(),
                documento.getNomeArquivo(),
                caminhoArquivo,
                documento.getTipoMime(),
                documento.getTamanhoBytes(),
                enviadoPorNome,
                documento.getCreatedAt());
    }
}
