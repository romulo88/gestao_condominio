package com.condominiogestao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Passo 1 do fluxo "Esqueci minha senha" - ver {@code AuthService.esqueciSenha}. */
public record EsqueciSenhaRequest(
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email) {
}
