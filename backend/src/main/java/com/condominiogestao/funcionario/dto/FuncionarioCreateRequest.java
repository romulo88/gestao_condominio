package com.condominiogestao.funcionario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Não recebe senha no cadastro - toda pessoa nasce com uma senha provisória e é avisada
 * por e-mail (quando tem um) de como acessar, via "Esqueci minha senha" (ver
 * {@code AuthService}/{@code SenhaProvisoriaService}). Mas nem todo funcionário tem um
 * vínculo com perfil (acesso ao sistema) - pra esse, e-mail não faz falta. Por isso o
 * e-mail é opcional AQUI (bate com {@code pessoas.email}, que é nullable no banco); quem
 * exige e-mail é {@link com.condominiogestao.funcionario.FuncionarioCondominioService},
 * e só quando o vínculo tem perfil de verdade (ver {@code criar}/{@code atualizar} lá).
 * {@code telefone} também é opcional, sem máscara (só dígitos).
 */
public record FuncionarioCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @Email(message = "email inválido") String email,
        String telefone) {
}
