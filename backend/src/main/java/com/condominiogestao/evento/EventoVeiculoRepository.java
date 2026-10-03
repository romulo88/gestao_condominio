package com.condominiogestao.evento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoVeiculoRepository extends JpaRepository<EventoVeiculo, Integer> {

    List<EventoVeiculo> findByEventoIdOrderById(Integer eventoId);

    /** Carga em lote pra listagem paginada da portaria - evita N+1 (uma query pra todos os
     * eventos da página, não uma por evento). */
    List<EventoVeiculo> findByEventoIdInOrderById(List<Integer> eventoIds);

    /** Alimenta o autocomplete de "visitante recorrente" (pedido do Romulo) - mesmo
     * espírito de {@code EventoPessoaRepository#findTop100ByEvento_Morador_IdOrderByIdDesc}. */
    List<EventoVeiculo> findTop100ByEvento_Morador_IdOrderByIdDesc(Integer moradorId);
}
