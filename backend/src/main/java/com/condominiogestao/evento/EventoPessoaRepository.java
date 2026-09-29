package com.condominiogestao.evento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPessoaRepository extends JpaRepository<EventoPessoa, Integer> {

    List<EventoPessoa> findByEventoIdOrderById(Integer eventoId);

    /** Carga em lote pra listagem paginada da portaria - evita N+1 (uma query pra todos os
     * eventos da página, não uma por evento). */
    List<EventoPessoa> findByEventoIdInOrderById(List<Integer> eventoIds);
}
