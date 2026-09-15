package com.condominiogestao.tarefaagendada;

import com.condominiogestao.common.Situacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaAgendadaRepository extends JpaRepository<TarefaAgendada, Integer> {

    /** Só as ativas de um condomínio - o que aparece na listagem/sininho (pedido do
     * Romulo: "remover" é soft-delete). A ordenação por "próxima data relevante" (a menor
     * das três datas) é feita no service - depende de {@link
     * TarefaAgendada#proximaDataRelevante()}, que não é uma coluna. */
    List<TarefaAgendada> findByCondominioIdAndSituacao(Integer condominioId, Situacao situacao);
}
