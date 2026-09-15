package com.condominiogestao.demanda;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandaAcompanhamentoRepository extends JpaRepository<DemandaAcompanhamento, Integer> {

    Optional<DemandaAcompanhamento> findByDemandaIdAndMoradorId(Integer demandaId, Integer moradorId);

    /** Tudo que esse morador acompanha - alimenta a consulta de "mudanças de status" no
     * login (ver {@code DemandaService.mudancasStatus}). */
    List<DemandaAcompanhamento> findByMoradorId(Integer moradorId);

    /** Só as linhas dentre um conjunto de demandas - `DemandaResponse.acompanhando` em
     * lote (`DemandaService.listar`), sem N+1. */
    List<DemandaAcompanhamento> findByMoradorIdAndDemandaIdIn(Integer moradorId, List<Integer> demandaIds);

    void deleteByDemandaIdAndMoradorId(Integer demandaId, Integer moradorId);
}
