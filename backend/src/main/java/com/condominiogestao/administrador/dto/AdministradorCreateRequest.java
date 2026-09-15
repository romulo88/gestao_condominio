package com.condominiogestao.administrador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Igual FuncionarioCreateRequest/MoradorCreateRequest - não recebe senha no cadastro. */
public record AdministradorCreateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "cpf é obrigatório") String cpf,
        @Email(message = "email inválido") String email) {
}
