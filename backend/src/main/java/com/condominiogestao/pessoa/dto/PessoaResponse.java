package com.condominiogestao.pessoa.dto;

import com.condominiogestao.pessoa.Pessoa;

/** Nunca expõe {@code senhaHash}. Usado pra "reconhecer" uma pessoa já cadastrada pelo
 * CPF antes de pedir nome/e-mail de novo (ex: aba Funcionário do cadastro de condomínio). */
public record PessoaResponse(Integer id, String nome, String cpf, String email) {

    public static PessoaResponse from(Pessoa pessoa) {
        return new PessoaResponse(pessoa.getId(), pessoa.getNome(), pessoa.getCpf(), pessoa.getEmail());
    }
}
