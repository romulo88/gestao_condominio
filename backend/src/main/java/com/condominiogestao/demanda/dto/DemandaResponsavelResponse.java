package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaResponsavel;
import com.condominiogestao.funcionario.FuncionarioCondominio;
import com.condominiogestao.funcionario.FuncionarioPerfil;

/** {@code perfil}/{@code funcao} vêm do vínculo do funcionário com o condomínio da demanda
 * (pedido do Romulo: mostrar ao lado do nome - cargo pra quem tem login, função pra quem
 * não tem) - {@code vinculo} pode ser {@code null} num caso raro (funcionário sem vínculo
 * ativo nesse condomínio no momento em que a lista é montada), aí os dois ficam nulos. */
public record DemandaResponsavelResponse(
        Integer id, Integer demandaId, Integer funcionarioId, String nome, FuncionarioPerfil perfil, String funcao) {

    public static DemandaResponsavelResponse from(DemandaResponsavel atribuicao, FuncionarioCondominio vinculo) {
        return new DemandaResponsavelResponse(
                atribuicao.getId(),
                atribuicao.getDemanda().getId(),
                atribuicao.getFuncionario().getId(),
                atribuicao.getFuncionario().getNome(),
                vinculo != null ? vinculo.getPerfil() : null,
                vinculo != null ? vinculo.getFuncao() : null);
    }
}
