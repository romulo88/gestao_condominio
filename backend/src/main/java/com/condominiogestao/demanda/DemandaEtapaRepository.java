package com.condominiogestao.demanda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandaEtapaRepository extends JpaRepository<DemandaEtapa, Integer> {

    List<DemandaEtapa> findByDemandaIdOrderByOrdem(Integer demandaId);

    /** Pra calcular `DemandaResponse.temEtapaVencida` em lote (ver `DemandaService`), sem N+1. */
    List<DemandaEtapa> findByDemandaIdIn(List<Integer> demandaIds);
}
