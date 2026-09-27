package com.condominiogestao.ronda.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Um ponto de GPS enviado pelo cliente - em lote, várias vezes durante a ronda (ver
 * {@code RondaController.registrarPontos}). {@code capturadoEm} é o horário real da
 * leitura no celular, não quando chega no servidor. */
public record RondaPontoRequest(
        @NotNull(message = "latitude é obrigatória") Double latitude,
        @NotNull(message = "longitude é obrigatória") Double longitude,
        @NotNull(message = "capturadoEm é obrigatório") LocalDateTime capturadoEm) {
}
