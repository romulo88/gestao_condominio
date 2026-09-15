package com.condominiogestao.tarefaagendada.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.tarefaagendada.TarefaAgendada;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TarefaAgendadaResponse(
        Integer id,
        Integer condominioId,
        Integer funcionarioId,
        String funcionarioNome,
        String titulo,
        String descricao,
        LocalDate dataTarefa,
        LocalDate dataPrimeiroAviso,
        LocalDate dataSegundoAviso,
        /** A menor das três datas - a listagem vem ordenada por ela (crescente). O cliente
         * usa as três datas cruas pra decidir o que é "hoje" (fuso do usuário). */
        LocalDate proximaDataRelevante,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TarefaAgendadaResponse from(TarefaAgendada tarefa) {
        return new TarefaAgendadaResponse(
                tarefa.getId(),
                tarefa.getCondominio().getId(),
                tarefa.getFuncionario().getId(),
                tarefa.getFuncionario().getNome(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getDataTarefa(),
                tarefa.getDataPrimeiroAviso(),
                tarefa.getDataSegundoAviso(),
                tarefa.proximaDataRelevante(),
                tarefa.getSituacao(),
                tarefa.getCreatedAt(),
                tarefa.getUpdatedAt());
    }
}
