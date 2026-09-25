package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Move uma demanda **já aprovada** de uma coluna do Kanban pra outra, E/OU reordena dentro
 * da mesma coluna (arrastar o card no quadro, pedido do Romulo: agrupar cards de assuntos
 * parecidos) - diferente de {@link DemandaAprovarRequest}, que é a primeira entrada na
 * coluna. Só gera uma nova linha em {@code DemandaStatusKanbanHistorico} quando a coluna
 * realmente muda - reordenar dentro da mesma coluna não é uma transição de status.
 *
 * <p>{@code antesDaDemandaId} é opcional: {@code null} solta no FIM da coluna de destino
 * (mesmo comportamento de sempre); preenchido, insere a demanda arrastada imediatamente
 * antes da demanda referenciada (que precisa já pertencer à coluna de destino) - é o que
 * alimenta "soltar encima de um card" no frontend, sem precisar calcular posição por
 * coordenada do mouse.
 */
public record DemandaMoverKanbanRequest(
        @NotNull(message = "statusKanbanId é obrigatório") Integer statusKanbanId, Integer antesDaDemandaId) {
}
