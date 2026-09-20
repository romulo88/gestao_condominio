package com.condominiogestao.demanda;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandaStatusKanbanHistoricoRepository
        extends JpaRepository<DemandaStatusKanbanHistorico, Integer> {

    List<DemandaStatusKanbanHistorico> findByDemandaIdOrderByCreatedAt(Integer demandaId);

    /** Desde quando cada demanda (dentre as informadas) está na coluna ATUAL dela - a
     * transição mais recente do histórico É a entrada na coluna atual. Usada em lote pelo
     * KPI de "dias parado" do Kanban ({@code DemandaService.buscarStatusKanbanDesdePorDemanda}),
     * sem N+1: uma linha por demanda (`MAX(createdAt)` agrupado), não o histórico inteiro. */
    @Query("SELECT h.demanda.id, MAX(h.createdAt) FROM DemandaStatusKanbanHistorico h "
            + "WHERE h.demanda.id IN :demandaIds GROUP BY h.demanda.id")
    List<Object[]> buscarUltimaTransicaoPorDemanda(@Param("demandaIds") List<Integer> demandaIds);

    /** Movimentações de coluna das demandas do morador depois de uma data - alerta de
     * mudança de status (pedido do Romulo). */
    List<DemandaStatusKanbanHistorico> findByDemandaMoradorSolicitanteIdAndCreatedAtAfterOrderByCreatedAt(
            Integer moradorSolicitanteId, LocalDateTime desde);

    /** Mesma coisa, só que pra um conjunto de ids específico - demandas que o morador
     * ACOMPANHA (funcionalidade "Acompanhar", v116), não as que ele abriu. */
    List<DemandaStatusKanbanHistorico> findByDemandaIdInAndCreatedAtAfterOrderByCreatedAt(
            List<Integer> demandaIds, LocalDateTime desde);

    /** Limpeza usada por {@code StatusKanbanService.excluir} - uma coluna sem card nela
     * agora ({@code DemandaRepository.existsByStatusKanbanId} = false) ainda pode ter
     * linha de histórico apontando pra ela (de um card que já passou por ali e se mudou
     * pra outra coluna depois) - como {@code id_status_novo} é NOT NULL no banco, não dá
     * pra só desvincular, tem que apagar a linha junto (é só rastro de auditoria, não
     * afeta o card em si, que já está registrado na coluna ATUAL dele). */
    void deleteByStatusAnteriorIdOrStatusNovoId(Integer statusAnteriorId, Integer statusNovoId);
}
