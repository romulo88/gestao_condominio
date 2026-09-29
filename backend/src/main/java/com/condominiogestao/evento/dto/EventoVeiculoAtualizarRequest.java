package com.condominiogestao.evento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code id} nulo = veículo novo (cria); preenchido = veículo já existente no evento -
 * ignorado pelo backend se ele já estiver liberado pela portaria (não pode mais editar -
 * ver {@code EventoService.sincronizarVeiculos}). */
public record EventoVeiculoAtualizarRequest(
        Integer id,
        @NotBlank(message = "placa é obrigatória") @Size(max = 8, message = "placa deve ter no máximo 8 caracteres")
                String placa) {
}
