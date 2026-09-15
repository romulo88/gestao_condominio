package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaResponsavel;

public record DemandaResponsavelResponse(Integer id, Integer demandaId, Integer funcionarioId, String nome, String cpf) {

    public static DemandaResponsavelResponse from(DemandaResponsavel atribuicao) {
        return new DemandaResponsavelResponse(
                atribuicao.getId(),
                atribuicao.getDemanda().getId(),
                atribuicao.getFuncionario().getId(),
                atribuicao.getFuncionario().getNome(),
                atribuicao.getFuncionario().getCpf());
    }
}
