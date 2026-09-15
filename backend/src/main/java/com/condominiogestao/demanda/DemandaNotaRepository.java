package com.condominiogestao.demanda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandaNotaRepository extends JpaRepository<DemandaNota, Integer> {

    /** Ordem cronológica - o frontend monta a árvore (raiz/resposta) a partir de
     * {@code notaPaiId}, então a ordem de chegada aqui só precisa estar correta dentro de
     * cada nível (mais antiga primeiro). */
    List<DemandaNota> findByDemandaIdOrderByCreatedAtAsc(Integer demandaId);

    /** Todas as notas de várias demandas de uma vez - usada pelo cálculo em lote de
     * {@code DemandaService.temNotaPendente} (ícone no card do Kanban), sem N+1. */
    List<DemandaNota> findByDemandaIdIn(List<Integer> demandaIds);
}
