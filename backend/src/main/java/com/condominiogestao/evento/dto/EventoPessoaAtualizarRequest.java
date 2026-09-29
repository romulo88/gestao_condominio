package com.condominiogestao.evento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code id} nulo = pessoa nova (cria); preenchido = pessoa já existente no evento -
 * ignorado pelo backend se ela já estiver liberada pela portaria (não pode mais editar -
 * ver {@code EventoService.sincronizarPessoas}). */
public record EventoPessoaAtualizarRequest(
        Integer id,
        @NotBlank(message = "nome é obrigatório") @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
                String nome,
        @NotBlank(message = "documento é obrigatório")
                @Size(max = 50, message = "documento deve ter no máximo 50 caracteres")
                String documento) {
}
