package com.condominiogestao.auth.dto;

import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.funcionario.FuncionarioPerfil;

/**
 * Uma combinação (condomínio, papel) que a pessoa pode escolher pra entrar - vem de uma
 * linha ativa de {@code FuncionarioCondominio} (com perfil preenchido) ou de
 * {@code MoradorCondominio}. {@code perfil} só existe quando {@code tipoPapel = funcionario}.
 */
public record ContextoDto(
        Integer condominioId, String condominioNome, TipoPessoa tipoPapel, FuncionarioPerfil perfil) {
}
