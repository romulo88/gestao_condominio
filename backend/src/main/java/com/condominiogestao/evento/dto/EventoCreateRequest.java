package com.condominiogestao.evento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** {@code espacoComumId} nulo = própria unidade do morador. {@code veiculos} pode vir
 * vazio (convidado a pé) - só {@code pessoas} exige pelo menos 1 (evento sem ninguém
 * esperado não faz sentido). */
public record EventoCreateRequest(
        Integer espacoComumId,
        @NotBlank(message = "motivo é obrigatório") @Size(max = 200, message = "motivo deve ter no máximo 200 caracteres")
                String motivo,
        @NotNull(message = "data é obrigatória") LocalDate data,
        @Size(max = 50, message = "horario deve ter no máximo 50 caracteres") String horario,
        @NotNull(message = "veiculos é obrigatório (pode ser uma lista vazia)") List<@Valid EventoVeiculoRequest> veiculos,
        @NotEmpty(message = "informe ao menos 1 pessoa") List<@Valid EventoPessoaRequest> pessoas) {
}
