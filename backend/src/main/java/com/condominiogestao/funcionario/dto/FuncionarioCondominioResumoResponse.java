package com.condominiogestao.funcionario.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.funcionario.FuncionarioCondominio;
import com.condominiogestao.funcionario.FuncionarioPerfil;

/**
 * Vínculo funcionário-condomínio já com nome/CPF/e-mail/foto embutidos (pedido do Romulo:
 * paginar a aba Funcionário do cadastro de condomínio) - antes disso, a listagem chamava
 * {@code GET /api/funcionarios/{id}} pra CADA vínculo à parte (N+1: 1 chamada da lista +
 * 1 por funcionário), só pra pegar esses mesmos dados. Aqui sai tudo numa consulta só (ver
 * {@code FuncionarioCondominioService.listarPaginaPorCondominio}).
 */
public record FuncionarioCondominioResumoResponse(
        Integer vinculoId,
        Integer funcionarioId,
        String nome,
        String cpf,
        String email,
        String fotoUrl,
        FuncionarioPerfil perfil,
        String funcao,
        Situacao situacao) {

    public static FuncionarioCondominioResumoResponse from(FuncionarioCondominio vinculo, String fotoUrl) {
        return new FuncionarioCondominioResumoResponse(
                vinculo.getId(),
                vinculo.getFuncionario().getId(),
                vinculo.getFuncionario().getNome(),
                vinculo.getFuncionario().getCpf(),
                vinculo.getFuncionario().getEmail(),
                fotoUrl,
                vinculo.getPerfil(),
                vinculo.getFuncao(),
                vinculo.getSituacao());
    }
}
