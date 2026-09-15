package com.condominiogestao.condominio.dto;

import com.condominiogestao.condominio.Bloco;

public record BlocoResponse(Integer id, Integer condominioId, String nome) {

    public static BlocoResponse from(Bloco bloco) {
        return new BlocoResponse(bloco.getId(), bloco.getCondominio().getId(), bloco.getNome());
    }
}
