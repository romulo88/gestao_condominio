package com.condominiogestao.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Passo 2 do fluxo "Esqueci minha senha" - e-mail identifica a pessoa (mesma checagem
 * do passo 1, repetida aqui por segurança), {@code senhaAtual} confirma que é ela mesma
 * quem está trocando. Ver {@code AuthService.trocarSenha}. */
public record TrocarSenhaRequest(
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email,
        @NotBlank(message = "senhaAtual é obrigatória") String senhaAtual,
        @NotBlank(message = "novaSenha é obrigatória")
                @Size(min = 8, message = "novaSenha deve ter no mínimo 8 caracteres")
                String novaSenha) {
}
