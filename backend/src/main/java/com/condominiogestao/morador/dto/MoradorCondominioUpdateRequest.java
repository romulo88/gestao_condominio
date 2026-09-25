package com.condominiogestao.morador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corrige bloco/unidade de um vínculo já existente e o e-mail/telefone da pessoa por trás
 * dele - trocar de morador ou de condomínio não é "editar", é um vínculo novo (ver
 * {@code MoradorCondominioCreateRequest}). Nome não entra aqui de propósito -
 * identidade não se edita por essa tela. {@code telefone} vem sem máscara (LGPD, v177) -
 * vazio/nulo apaga o telefone existente, diferente do e-mail (que nunca é apagado, só
 * trocado). */
public record MoradorCondominioUpdateRequest(
        Integer blocoId,
        @NotBlank(message = "numeroUnidade é obrigatório") String numeroUnidade,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        String telefone) {
}
