package com.condominiogestao.kanban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StatusKanbanCreateRequest(
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        @NotBlank(message = "nome é obrigatório") String nome,
        Integer ordem,
        /** Null (campo omitido) vira true - mesmo default do banco (V7). */
        Boolean visivelExternamente,
        /** Null (campo omitido) vira false - mesmo default do banco (V9). */
        Boolean finalistico) {
}
