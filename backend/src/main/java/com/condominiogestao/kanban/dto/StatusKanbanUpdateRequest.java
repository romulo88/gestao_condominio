package com.condominiogestao.kanban.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusKanbanUpdateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        Integer ordem,
        /** Null (campo omitido) não mexe no valor atual - mesmo critério de {@code ordem}. */
        Boolean visivelExternamente,
        /** Null (campo omitido) não mexe no valor atual - mesmo critério de {@code ordem}. */
        Boolean finalistico,
        /** Null (campo omitido) não mexe no valor atual - mesmo critério de {@code ordem}. */
        Boolean recorrente) {
}
