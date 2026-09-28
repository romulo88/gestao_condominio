package com.condominiogestao.ronda.dto;

import com.condominiogestao.ronda.TipoRonda;
import jakarta.validation.constraints.Size;

/** Corpo de `PATCH /api/rondas/{id}/finalizar` - só vale quando quem finaliza é o próprio
 * rondista (dono da ronda), e nesse caso {@code tipo} é obrigatório (checado em
 * {@code RondaService.finalizar}, não aqui: o mesmo endpoint também é usado por um perfil
 * completo encerrando uma ronda esquecida de outro rondista, sem corpo nenhum - nesse caso o
 * corpo é ignorado). {@code observacao} é opcional (máx. 100 caracteres, ex.: "Acompanhando
 * entregador até a casa 300"). */
public record RondaFinalizarRequest(
        TipoRonda tipo,
        @Size(max = 100, message = "observacao deve ter no máximo 100 caracteres") String observacao) {
}
