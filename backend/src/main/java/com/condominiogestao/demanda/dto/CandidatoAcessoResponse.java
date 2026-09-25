package com.condominiogestao.demanda.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/**
 * Uma pessoa (morador ou funcionário) ativa no condomínio da demanda - alimenta a combo
 * de busca do "Gerenciar acesso" (item 4.8), pra escolher por nome. {@code pessoaId} é o
 * identificador enviado de volta ao selecionar (CPF saiu do sistema, v177 - LGPD; como
 * {@code Morador}/{@code Funcionario} compartilham a mesma PK de {@code Pessoa}, esse id
 * já é o {@code moradorId}/{@code funcionarioId} certo pra qualquer um dos dois papéis).
 * {@code unidade}/{@code blocoNome} só vêm preenchidos pra morador (separados pra tela
 * montar "Nome (unidade)" ou "Nome (bloco - unidade)" - {@code blocoNome} é {@code null}
 * quando o vínculo não tem bloco) - funcionário não mora lá. {@code perfil}/{@code funcao}
 * só vêm preenchidos pra funcionário (pedido do Romulo: mostrar o cargo - Síndico/Sub-
 * síndico/etc. - pra quem tem login, ou a função - jardineiro, rondista - pra quem não
 * tem) - morador não tem nenhum dos dois.
 */
public record CandidatoAcessoResponse(
        Integer pessoaId,
        String nome,
        String tipoPessoa,
        String unidade,
        String blocoNome,
        FuncionarioPerfil perfil,
        String funcao) {}
