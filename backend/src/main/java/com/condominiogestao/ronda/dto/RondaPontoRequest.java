package com.condominiogestao.ronda.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Um ponto de GPS enviado pelo cliente - em lote, várias vezes durante a ronda (ver
 * {@code RondaController.registrarPontos}). {@code capturadoEm} é o horário real da
 * leitura no celular, não quando chega no servidor.
 *
 * <p>{@link Instant} (o cliente manda ISO com "Z", `Date.toISOString()`) e não
 * {@code LocalDateTime}: o Jackson descarta o "Z" ao ler um {@code LocalDateTime}, e o ponto
 * ficava gravado em UTC (3h à frente) enquanto o resto da ronda (`iniciadaEm` etc.) usa o
 * horário do servidor - {@code RondaService.registrarPontos} converte pro fuso do servidor. */
public record RondaPontoRequest(
        @NotNull(message = "latitude é obrigatória") Double latitude,
        @NotNull(message = "longitude é obrigatória") Double longitude,
        @NotNull(message = "capturadoEm é obrigatório") Instant capturadoEm) {
}
