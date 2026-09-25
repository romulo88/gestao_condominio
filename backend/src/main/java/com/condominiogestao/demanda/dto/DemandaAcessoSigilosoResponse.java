package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaAcessoSigiloso;

public record DemandaAcessoSigilosoResponse(
        Integer id,
        Integer demandaId,
        /** "morador" ou "funcionario". */
        String tipoPessoa,
        String nome) {

    public static DemandaAcessoSigilosoResponse from(DemandaAcessoSigiloso acesso) {
        boolean ehMorador = acesso.getMorador() != null;
        return new DemandaAcessoSigilosoResponse(
                acesso.getId(),
                acesso.getDemanda().getId(),
                acesso.getTipoPessoa().name(),
                ehMorador ? acesso.getMorador().getNome() : acesso.getFuncionario().getNome());
    }
}
