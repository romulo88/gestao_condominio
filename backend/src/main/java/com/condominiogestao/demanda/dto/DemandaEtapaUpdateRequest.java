package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record DemandaEtapaUpdateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        LocalDate prazo,
        Integer ordem) {
}
