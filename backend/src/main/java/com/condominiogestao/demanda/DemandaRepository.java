package com.condominiogestao.demanda;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandaRepository extends JpaRepository<Demanda, Integer> {

    List<Demanda> findByCondominioId(Integer condominioId);

    /** Visão do morador: só as demandas que ele mesmo abriu, nunca as dos vizinhos. */
    List<Demanda> findByCondominioIdAndMoradorSolicitanteId(Integer condominioId, Integer moradorSolicitanteId);

    /** Demandas do morador decididas (aprovada OU reprovada - `dataAprovacao` é gravada
     * nos dois casos, ver `DemandaService`) depois de uma data - alerta de mudança de
     * status (pedido do Romulo). */
    List<Demanda> findByMoradorSolicitanteIdAndDataAprovacaoAfter(Integer moradorSolicitanteId, LocalDateTime desde);

    /** Mesma coisa que {@link #findByMoradorSolicitanteIdAndDataAprovacaoAfter}, só que
     * pra um conjunto de ids específico em vez do solicitante - usada pelas demandas que
     * o morador ACOMPANHA (funcionalidade "Acompanhar", v116), não as que ele abriu. */
    List<Demanda> findByIdInAndDataAprovacaoAfter(List<Integer> ids, LocalDateTime desde);

    /** Usado por {@code StatusKanbanService.excluir} (pedido do Romulo) - só deixa excluir
     * uma coluna que não tem NENHUM card nela no momento. */
    boolean existsByStatusKanbanId(Integer statusKanbanId);

    /** Todos os cards de uma coluna do Kanban, na ordem manual atual - usado por {@code
     * DemandaService.moverKanban}/{@code aprovar} pra inserir/reordenar um card e
     * renumerar o resto da coluna (pedido do Romulo: arrastar card pra qualquer posição). */
    List<Demanda> findByStatusKanbanIdOrderByOrdemAsc(Integer statusKanbanId);

    /** Total de demandas abertas durante um conjunto de rondas - usado no resumo agregado
     * da tela "Rondas" do síndico (feature "Controle de Rondas"). */
    long countByRondaIdIn(List<Integer> rondaIds);

    /** Contagem de demandas POR ronda (uma linha por `id_ronda` com pelo menos 1 demanda) -
     * usado na listagem paginada da tela "Rondas", em lote pra evitar N+1 (mesmo espírito
     * de {@code DemandaResponsavelRepository.findByDemandaIdIn}). Cada `Object[]` é
     * `[id_ronda (Integer), total (Long)]`. */
    @Query("SELECT d.ronda.id, COUNT(d) FROM Demanda d WHERE d.ronda.id IN :rondaIds GROUP BY d.ronda.id")
    List<Object[]> contarPorRondaId(@Param("rondaIds") List<Integer> rondaIds);
}
