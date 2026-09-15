package com.condominiogestao.demanda;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
