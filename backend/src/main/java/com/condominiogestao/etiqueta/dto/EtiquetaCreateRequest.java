package com.condominiogestao.etiqueta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code condominioId} explícito (e não do contexto): administrador cadastra etiqueta
 * "padrão" em QUALQUER condomínio (aba Etiquetas do cadastro de condomínio), não só no
 * próprio - mesma necessidade que já existia em {@code StatusKanbanCreateRequest}. Ainda
 * assim nunca confiar cegamente nele: o service sempre valida contra
 * {@link com.condominiogestao.common.Autorizacao#exigirAdministradorOuFuncionarioDoCondominio}. */
public record EtiquetaCreateRequest(
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        @NotBlank(message = "descricao é obrigatória")
                @Size(max = 50, message = "descricao deve ter no máximo 50 caracteres")
                String descricao,
        @NotBlank(message = "cor é obrigatória") String cor,
        /** Null (campo omitido) vira true - mesmo default do banco (V18). */
        Boolean visivelMorador) {
}
