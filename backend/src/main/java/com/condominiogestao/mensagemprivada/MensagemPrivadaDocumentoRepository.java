package com.condominiogestao.mensagemprivada;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemPrivadaDocumentoRepository extends JpaRepository<MensagemPrivadaDocumento, Integer> {

    List<MensagemPrivadaDocumento> findByMensagemId(Integer mensagemId);

    /** Em lote pra listagem/detalhe de várias mensagens de uma vez, sem N+1. */
    List<MensagemPrivadaDocumento> findByMensagemIdIn(List<Integer> mensagemIds);

    /** Limite de fotos por MENSAGEM (pedido do Romulo - parametrizável, ver
     * {@code ParametroService}, chave {@code mensagemPrivadaMaximoFotos}), mesmo padrão de
     * {@code DemandaDocumentoRepository.countByDemandaIdAndTipoMimeStartingWith}. */
    long countByMensagemIdAndTipoMimeStartingWith(Integer mensagemId, String prefixoTipoMime);
}
