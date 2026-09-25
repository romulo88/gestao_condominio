package com.condominiogestao.pessoa.dto;

import com.condominiogestao.pessoa.Pessoa;

/** Nunca expõe {@code senhaHash}. Usado pra "reconhecer" uma pessoa já cadastrada pelo
 * e-mail antes de pedir nome/telefone de novo (ex: aba Funcionário do cadastro de
 * condomínio). */
public record PessoaResponse(Integer id, String nome, String email, String telefone) {

    public static PessoaResponse from(Pessoa pessoa) {
        return new PessoaResponse(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getTelefone());
    }
}
