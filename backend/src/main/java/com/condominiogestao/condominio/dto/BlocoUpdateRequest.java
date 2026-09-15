package com.condominiogestao.condominio.dto;

import jakarta.validation.constraints.NotBlank;

/** Só corrige o nome - trocar de condomínio não é "editar", é bloco novo (ver
 * {@code BlocoCreateRequest}). */
public record BlocoUpdateRequest(@NotBlank(message = "nome é obrigatório") String nome) {
}
