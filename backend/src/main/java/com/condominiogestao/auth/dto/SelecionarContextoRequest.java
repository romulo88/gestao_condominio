package com.condominiogestao.auth.dto;

import com.condominiogestao.common.TipoPessoa;
import jakarta.validation.constraints.NotNull;

/**
 * {@code condominioId} não é {@code @NotNull} de propósito: pra {@code tipoPapel =
 * administrador} ele vem nulo (papel global, sem condomínio - ver AuthService).
 */
public record SelecionarContextoRequest(
        Integer condominioId, @NotNull(message = "tipoPapel é obrigatório") TipoPessoa tipoPapel) {
}
