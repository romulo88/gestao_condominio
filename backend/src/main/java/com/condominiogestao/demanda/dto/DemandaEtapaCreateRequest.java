package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** {@code prazo} é só data (sem hora) e opcional - nem toda etapa precisa de prazo marcado. */
public record DemandaEtapaCreateRequest(
        @NotNull(message = "demandaId é obrigatório") Integer demandaId,
        @NotBlank(message = "nome é obrigatório") String nome,
        LocalDate prazo,
        Integer ordem) {
}
