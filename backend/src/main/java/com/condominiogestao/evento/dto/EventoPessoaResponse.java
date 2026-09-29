package com.condominiogestao.evento.dto;

import com.condominiogestao.evento.EventoPessoa;
import java.time.LocalDateTime;

public record EventoPessoaResponse(
        Integer id, String nome, String documento, boolean liberado, String liberadoPorNome, LocalDateTime liberadoEm) {

    public static EventoPessoaResponse from(EventoPessoa pessoa) {
        return new EventoPessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getDocumento(),
                pessoa.isLiberado(),
                pessoa.getFuncionarioLiberou() == null ? null : pessoa.getFuncionarioLiberou().getNome(),
                pessoa.getLiberadoEm());
    }
}
