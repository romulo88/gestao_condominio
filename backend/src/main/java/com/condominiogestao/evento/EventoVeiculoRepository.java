package com.condominiogestao.evento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoVeiculoRepository extends JpaRepository<EventoVeiculo, Integer> {

    List<EventoVeiculo> findByEventoIdOrderById(Integer eventoId);

    /** Carga em lote pra listagem paginada da portaria - evita N+1 (uma query pra todos os
     * eventos da página, não uma por evento). */
    List<EventoVeiculo> findByEventoIdInOrderById(List<Integer> eventoIds);
}
