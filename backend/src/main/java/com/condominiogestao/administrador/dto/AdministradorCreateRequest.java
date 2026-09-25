package com.condominiogestao.administrador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Igual FuncionarioCreateRequest/MoradorCreateRequest - não recebe senha no cadastro.
 * Diferente de funcionário (onde e-mail é opcional pra quem não tem perfil de acesso),
 * administrador SEMPRE loga - então e-mail é obrigatório aqui (é o identificador de
 * login/"esqueci minha senha" desde que CPF saiu do sistema, v177). */
public record AdministradorCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "email é obrigatório") @Email(message = "email inválido") String email) {
}
