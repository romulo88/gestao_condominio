package com.condominiogestao.funcionario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Não recebe senha no cadastro - toda pessoa nasce com a senha padrão
 * ({@code auth.senha-padrao}) e troca pelo fluxo guiado de "Esqueci minha senha" (ver
 * {@code AuthService}), fluxo esse que depende de CPF + e-mail. Mas nem todo funcionário
 * tem um vínculo com perfil (acesso ao sistema) - pra esse, e-mail não faz falta. Por isso
 * o e-mail é opcional AQUI (bate com {@code pessoas.email}, que é nullable no banco); quem
 * exige e-mail é {@link com.condominiogestao.funcionario.FuncionarioCondominioService},
 * e só quando o vínculo tem perfil de verdade (ver {@code criar}/{@code atualizar} lá).
 */
public record FuncionarioCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "cpf é obrigatório") String cpf,
        @Email(message = "email inválido") String email) {
}
