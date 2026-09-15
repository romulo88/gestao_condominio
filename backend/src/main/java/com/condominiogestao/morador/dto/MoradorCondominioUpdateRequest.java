package com.condominiogestao.morador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corrige bloco/unidade de um vínculo já existente e o e-mail da pessoa por trás dele -
 * trocar de morador ou de condomínio não é "editar", é um vínculo novo (ver
 * {@code MoradorCondominioCreateRequest}). Nome/CPF não entram aqui de propósito -
 * identidade não se edita por essa tela. */
public record MoradorCondominioUpdateRequest(
        Integer blocoId,
        @NotBlank(message = "numeroUnidade é obrigatório") String numeroUnidade,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email) {
}
