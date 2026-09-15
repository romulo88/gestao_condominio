package com.condominiogestao.parametro.dto;

import jakarta.validation.constraints.NotBlank;

/** Só corrige descrição/valor - {@code nome} não é editável (ver {@link
 * com.condominiogestao.parametro.Parametro}). */
public record ParametroUpdateRequest(String descricao, @NotBlank(message = "valor é obrigatório") String valor) {
}
