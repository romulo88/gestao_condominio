package com.condominiogestao.ronda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RondaPontoRepository extends JpaRepository<RondaPonto, Integer> {

    /** Ordem cronológica - usado tanto pro cálculo de distância (Haversine entre pontos
     * consecutivos) quanto pro desenho do trajeto no mapa. */
    List<RondaPonto> findByRondaIdOrderByCapturadoEmAsc(Integer rondaId);
}
