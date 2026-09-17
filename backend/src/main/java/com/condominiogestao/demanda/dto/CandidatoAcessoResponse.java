package com.condominiogestao.demanda.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/**
 * Uma pessoa (morador ou funcionário) ativa no condomínio da demanda - alimenta a combo
 * de busca do "Gerenciar acesso" (item 4.8), pra escolher por nome em vez de digitar o CPF
 * de cabeça. {@code unidade} só vem preenchida pra morador (bloco + número, ou só número
 * quando o condomínio não tem bloco) - funcionário não mora lá. {@code perfil}/{@code
 * funcao} só vêm preenchidos pra funcionário (pedido do Romulo: mostrar o cargo - Síndico/
 * Sub-síndico/etc. - pra quem tem login, ou a função - jardineiro, rondista - pra quem
 * não tem, em vez do CPF na sugestão) - morador não tem nenhum dos dois.
 */
public record CandidatoAcessoResponse(
        String cpf, String nome, String tipoPessoa, String unidade, FuncionarioPerfil perfil, String funcao) {}
