package com.condominiogestao.mensagemprivada;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversaPrivadaVisualizacaoRepository extends JpaRepository<ConversaPrivadaVisualizacao, Integer> {

    /** Tela de investigação (auditoria), não precisa do histórico completo de uma vez -
     * `conversas_privadas_visualizacoes` é append-only (1 linha por abertura/envio, ver
     * ConversaPrivadaService.registrarVisualizacao), cresce sem teto numa conversa de longa
     * duração. As 200 mais recentes bastam pra quem tá investigando. */
    List<ConversaPrivadaVisualizacao> findTop200ByConversaIdOrderByVisualizadoEmDesc(Integer conversaId);
}
