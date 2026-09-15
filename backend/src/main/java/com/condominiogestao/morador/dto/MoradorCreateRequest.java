package com.condominiogestao.morador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Não recebe senha no cadastro - nasce com a senha padrão ({@code auth.senha-padrao})
 * e troca pelo fluxo guiado de "Esqueci minha senha" (ver {@code AuthService}), que usa
 * CPF + e-mail pra identificar a pessoa - por isso o e-mail é obrigatório aqui.
 */
public record MoradorCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "cpf é obrigatório") String cpf,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email) {
}
