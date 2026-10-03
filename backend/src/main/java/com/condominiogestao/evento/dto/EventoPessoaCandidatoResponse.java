package com.condominiogestao.evento.dto;

/** Sugestão de autocomplete (pedido do Romulo - "visitante recorrente") - nome+documento já
 * usados pelo PRÓPRIO morador logado em algum evento anterior dele, nunca de outro morador.
 * Ver {@code EventoService#listarCandidatosPessoas}. */
public record EventoPessoaCandidatoResponse(String nome, String documento) {
}
