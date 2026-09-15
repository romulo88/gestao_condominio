package com.condominiogestao.tarefaagendada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Não recebe {@code condominioId} nem {@code funcionarioId} - os dois vêm do contexto do
 * login (o funcionário que está criando, no condomínio dele). As três datas são puras
 * (sem hora) e obrigatórias. Ordem entre elas é validada (pedido do Romulo, ver
 * {@code TarefaAgendadaService.validarDatas}):
 * {@code dataPrimeiroAviso < dataSegundoAviso < dataTarefa}.
 */
public record TarefaAgendadaCreateRequest(
        @NotBlank(message = "titulo é obrigatório") String titulo,
        @NotBlank(message = "descricao é obrigatória") String descricao,
        @NotNull(message = "dataTarefa é obrigatória") LocalDate dataTarefa,
        @NotNull(message = "dataPrimeiroAviso é obrigatória") LocalDate dataPrimeiroAviso,
        @NotNull(message = "dataSegundoAviso é obrigatória") LocalDate dataSegundoAviso) {
}
