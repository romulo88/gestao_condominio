package com.condominiogestao.termos.dto;

/** Não nulo em {@code LoginResponse.termosPendente} só quando a pessoa precisa aceitar uma
 * versão nova (ou nunca aceitou nenhuma) - ver {@code TermosResponsabilidade}. */
public record TermosPendenteResponse(int versao, String texto) {
}
