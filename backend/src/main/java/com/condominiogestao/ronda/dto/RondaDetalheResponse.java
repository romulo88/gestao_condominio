package com.condominiogestao.ronda.dto;

import com.condominiogestao.ronda.Ronda;
import com.condominiogestao.ronda.RondaPonto;
import com.condominiogestao.ronda.RondaStatus;
import java.time.LocalDateTime;
import java.util.List;

/** {@link RondaResponse} + o trajeto inteiro - usado no mapa ao clicar numa ronda (rondista
 * conferindo a própria, síndico ou morador conferindo qualquer uma do condomínio).
 *
 * <p>{@code funcionarioNome} - mesma regra de {@link RondaResponse}: só perfil completo vê
 * quem fez a ronda, os demais só pelo {@code id}. */
public record RondaDetalheResponse(
        Integer id,
        Integer funcionarioId,
        String funcionarioNome,
        LocalDateTime iniciadaEm,
        LocalDateTime finalizadaEm,
        RondaStatus status,
        Double distanciaMetros,
        long totalDemandas,
        List<RondaPontoResponse> pontos) {

    public static RondaDetalheResponse from(
            Ronda ronda, long totalDemandas, List<RondaPonto> pontos, boolean podeVerNomeFuncionario) {
        return new RondaDetalheResponse(
                ronda.getId(),
                ronda.getFuncionario().getId(),
                podeVerNomeFuncionario ? ronda.getFuncionario().getNome() : null,
                ronda.getIniciadaEm(),
                ronda.getFinalizadaEm(),
                ronda.getStatus(),
                ronda.getDistanciaMetros(),
                totalDemandas,
                pontos.stream().map(RondaPontoResponse::from).toList());
    }
}
