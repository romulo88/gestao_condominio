package com.condominiogestao.tarefaagendada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Corrige título/descrição/datas de uma tarefa já cadastrada - mesmos campos e mesmas
 * regras de {@code TarefaAgendadaCreateRequest} (ordem entre as três datas validada). Trocar
 * de condomínio não é "editar", é tarefa nova. */
public record TarefaAgendadaUpdateRequest(
        @NotBlank(message = "titulo é obrigatório") String titulo,
        @NotBlank(message = "descricao é obrigatória") String descricao,
        @NotNull(message = "dataTarefa é obrigatória") LocalDate dataTarefa,
        @NotNull(message = "dataPrimeiroAviso é obrigatória") LocalDate dataPrimeiroAviso,
        @NotNull(message = "dataSegundoAviso é obrigatória") LocalDate dataSegundoAviso) {
}
