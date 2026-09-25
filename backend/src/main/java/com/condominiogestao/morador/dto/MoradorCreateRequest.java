package com.condominiogestao.morador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Não recebe senha no cadastro - nasce com uma senha provisória e é avisado por e-mail
 * de como acessar, via "Esqueci minha senha" (ver {@code AuthService}/
 * {@code SenhaProvisoriaService}), que usa e-mail pra identificar a pessoa - por isso o
 * e-mail é obrigatório aqui. {@code telefone} é opcional, sem máscara (só dígitos).
 */
public record MoradorCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        String telefone) {
}
