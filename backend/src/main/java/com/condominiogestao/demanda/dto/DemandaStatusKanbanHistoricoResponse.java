package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaStatusKanbanHistorico;
import java.time.LocalDateTime;

/** Uma linha do histórico de transição de coluna do Kanban (ver
 * {@code DemandaStatusKanbanHistorico}) - usado pelo ícone de relógio do card do Kanban:
 * a última linha da lista (mais recente) diz desde quando a demanda está na coluna atual,
 * e a lista inteira é o histórico completo mostrado ao clicar. */
public record DemandaStatusKanbanHistoricoResponse(
        Integer id,
        Integer demandaId,
        /** Null na primeira transição (entrada no Kanban via aprovação) - não tinha coluna anterior. */
        Integer statusAnteriorId,
        String statusAnteriorNome,
        Integer statusNovoId,
        String statusNovoNome,
        String funcionarioNome,
        LocalDateTime createdAt) {

    public static DemandaStatusKanbanHistoricoResponse from(DemandaStatusKanbanHistorico historico) {
        return new DemandaStatusKanbanHistoricoResponse(
                historico.getId(),
                historico.getDemanda().getId(),
                historico.getStatusAnterior() != null ? historico.getStatusAnterior().getId() : null,
                historico.getStatusAnterior() != null ? historico.getStatusAnterior().getNome() : null,
                historico.getStatusNovo().getId(),
                historico.getStatusNovo().getNome(),
                historico.getFuncionario().getNome(),
                historico.getCreatedAt());
    }
}
