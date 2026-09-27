package com.condominiogestao.ronda.dto;

/** Resumo do período filtrado na tela "Rondas" do síndico - total de rondas, tempo total
 * em ronda (soma de todas, inclusive as em andamento contando até agora) e total de
 * demandas abertas durante elas. */
public record RondaResumoResponse(long totalRondas, long tempoTotalSegundos, long totalDemandas) {
}
