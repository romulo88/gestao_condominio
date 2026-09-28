package com.condominiogestao.ronda.dto;

import com.condominiogestao.ronda.Ronda;
import com.condominiogestao.ronda.RondaPonto;
import com.condominiogestao.ronda.RondaStatus;
import com.condominiogestao.ronda.TipoRonda;
import java.time.LocalDateTime;
import java.util.List;

/** {@link RondaResponse} + o trajeto inteiro - usado no mapa ao clicar numa ronda (rondista
 * conferindo a própria, síndico ou morador conferindo qualquer uma do condomínio).
 *
 * <p>{@code funcionarioNome}/{@code observacao} - mesma regra de {@link RondaResponse}: só
 * perfil completo vê quem fez a ronda; a observação, perfil completo e o próprio rondista;
 * morador só vê {@code id} e {@code tipo}. */
public record RondaDetalheResponse(
        Integer id,
        Integer funcionarioId,
        String funcionarioNome,
        LocalDateTime iniciadaEm,
        LocalDateTime finalizadaEm,
        RondaStatus status,
        Double distanciaMetros,
        long totalDemandas,
        TipoRonda tipo,
        String observacao,
        List<RondaPontoResponse> pontos) {

    public static RondaDetalheResponse from(
            Ronda ronda, long totalDemandas, List<RondaPonto> pontos, boolean perfilCompleto, boolean podeVerObservacao) {
        return new RondaDetalheResponse(
                ronda.getId(),
                ronda.getFuncionario().getId(),
                perfilCompleto ? ronda.getFuncionario().getNome() : null,
                ronda.getIniciadaEm(),
                ronda.getFinalizadaEm(),
                ronda.getStatus(),
                ronda.getDistanciaMetros(),
                totalDemandas,
                ronda.getTipo(),
                podeVerObservacao ? ronda.getObservacao() : null,
                pontos.stream().map(RondaPontoResponse::from).toList());
    }
}
