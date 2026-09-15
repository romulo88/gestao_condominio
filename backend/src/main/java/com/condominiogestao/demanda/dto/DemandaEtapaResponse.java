package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaEtapa;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DemandaEtapaResponse(
        Integer id,
        Integer demandaId,
        String nome,
        /** Só data, opcional - null quando a etapa não tem prazo marcado. */
        LocalDate prazo,
        boolean concluida,
        LocalDateTime concluidaEm,
        Integer ordem) {

    public static DemandaEtapaResponse from(DemandaEtapa etapa) {
        return new DemandaEtapaResponse(
                etapa.getId(),
                etapa.getDemanda().getId(),
                etapa.getNome(),
                etapa.getPrazo(),
                etapa.isConcluida(),
                etapa.getConcluidaEm(),
                etapa.getOrdem());
    }
}
