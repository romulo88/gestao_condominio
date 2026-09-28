package com.condominiogestao.ronda.dto;

import com.condominiogestao.ronda.Ronda;
import com.condominiogestao.ronda.RondaStatus;
import com.condominiogestao.ronda.TipoRonda;
import java.time.LocalDateTime;

/** {@code totalDemandas} vem calculado por quem monta a resposta (ver
 * {@code RondaService} - em lote via {@code DemandaRepository.contarPorRondaId} nas
 * listagens, pra evitar N+1), não é uma coluna de {@link Ronda}. Duração não é persistida -
 * quem exibe calcula {@code finalizadaEm - iniciadaEm} (ou `agora - iniciadaEm` enquanto
 * `em_andamento`).
 *
 * <p>{@code funcionarioNome} (pedido do Romulo): só quem gerencia (perfil completo -
 * síndico/sub-síndico/encarregado/supervisor) vê QUEM fez a ronda. {@code observacao} (o que
 * o rondista escreveu ao finalizar): perfil completo E o próprio rondista (lembrete pra ele,
 * a lista dele já é só das próprias rondas); morador não vê. Morador vê a ronda só pelo
 * {@code id} e pelo {@code tipo} (mesmo espírito de {@code Demanda.identificarSolicitante}:
 * o backend já manda `null`, não é só a tela que escolhe esconder). {@code id} continua sempre visível pros 3 públicos - é como o morador
 * referencia uma ronda específica numa reclamação ("a ronda #7 não passou pelo bloco B").
 * {@code tipo} vem `null` em ronda em andamento ou encerrada por outra pessoa/pelo sistema. */
public record RondaResponse(
        Integer id,
        Integer funcionarioId,
        String funcionarioNome,
        LocalDateTime iniciadaEm,
        LocalDateTime finalizadaEm,
        RondaStatus status,
        Double distanciaMetros,
        long totalDemandas,
        TipoRonda tipo,
        String observacao) {

    public static RondaResponse from(Ronda ronda, long totalDemandas, boolean perfilCompleto, boolean podeVerObservacao) {
        return new RondaResponse(
                ronda.getId(),
                ronda.getFuncionario().getId(),
                perfilCompleto ? ronda.getFuncionario().getNome() : null,
                ronda.getIniciadaEm(),
                ronda.getFinalizadaEm(),
                ronda.getStatus(),
                ronda.getDistanciaMetros(),
                totalDemandas,
                ronda.getTipo(),
                podeVerObservacao ? ronda.getObservacao() : null);
    }
}
