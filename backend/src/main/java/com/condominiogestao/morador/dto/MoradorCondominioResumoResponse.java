package com.condominiogestao.morador.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.morador.MoradorCondominio;

/**
 * Vínculo morador-condomínio já com nome/e-mail/telefone embutidos - mesmo espírito de
 * {@code FuncionarioCondominioResumoResponse} (pedido do Romulo: paginar a aba Morador do
 * cadastro de condomínio, eliminando o N+1 que a listagem tinha antes).
 */
public record MoradorCondominioResumoResponse(
        Integer vinculoId,
        Integer moradorId,
        String nome,
        String email,
        String telefone,
        Integer blocoId,
        String numeroUnidade,
        Situacao situacao) {

    public static MoradorCondominioResumoResponse from(MoradorCondominio vinculo) {
        return new MoradorCondominioResumoResponse(
                vinculo.getId(),
                vinculo.getMorador().getId(),
                vinculo.getMorador().getNome(),
                vinculo.getMorador().getEmail(),
                vinculo.getMorador().getTelefone(),
                vinculo.getBloco() != null ? vinculo.getBloco().getId() : null,
                vinculo.getNumeroUnidade(),
                vinculo.getSituacao());
    }
}
