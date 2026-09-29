package com.condominiogestao.evento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Diferente de {@link EventoCreateRequest}: cada item de {@code veiculos}/{@code pessoas}
 * tem um {@code id} opcional (nulo = item novo; preenchido = item existente a atualizar) -
 * o backend sincroniza a lista (atualiza quem tem id, cria quem não tem, apaga quem não
 * veio na lista) em vez de substituir tudo, pra poder preservar itens já liberados pela
 * portaria intactos mesmo que não venham na lista (ver {@code EventoService.atualizar}).
 * Por isso {@code pessoas} pode vir vazia aqui (diferente do create) - o total de pessoas
 * do evento (liberadas + as desta lista) é quem precisa ser >= 1, checado no service.
 */
public record EventoUpdateRequest(
        Integer espacoComumId,
        @NotBlank(message = "motivo é obrigatório") @Size(max = 200, message = "motivo deve ter no máximo 200 caracteres")
                String motivo,
        @NotNull(message = "data é obrigatória") LocalDate data,
        @Size(max = 50, message = "horario deve ter no máximo 50 caracteres") String horario,
        @NotNull(message = "veiculos é obrigatório (pode ser uma lista vazia)")
                List<@Valid EventoVeiculoAtualizarRequest> veiculos,
        @NotNull(message = "pessoas é obrigatório (pode ser uma lista vazia)")
                List<@Valid EventoPessoaAtualizarRequest> pessoas) {
}
