package com.condominiogestao.etiqueta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corrige texto/cor/visibilidade pro morador - trocar de condomínio não é "editar", é
 * etiqueta nova (ver {@code EtiquetaCreateRequest}). */
public record EtiquetaUpdateRequest(
        @NotBlank(message = "descricao é obrigatória")
                @Size(max = 50, message = "descricao deve ter no máximo 50 caracteres")
                String descricao,
        @NotBlank(message = "cor é obrigatória") String cor,
        /** Null (campo omitido) não mexe no valor atual - mesmo critério de {@code StatusKanbanUpdateRequest}. */
        Boolean visivelMorador) {
}
