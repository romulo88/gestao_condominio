package com.condominiogestao.ronda.dto;

import com.condominiogestao.ronda.RondaPonto;
import java.time.LocalDateTime;

public record RondaPontoResponse(Double latitude, Double longitude, LocalDateTime capturadoEm) {

    public static RondaPontoResponse from(RondaPonto ponto) {
        return new RondaPontoResponse(ponto.getLatitude(), ponto.getLongitude(), ponto.getCapturadoEm());
    }
}
