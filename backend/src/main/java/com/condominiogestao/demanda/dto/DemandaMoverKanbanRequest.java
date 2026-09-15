package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Move uma demanda **já aprovada** de uma coluna do Kanban pra outra (arrastar o card no
 * quadro) - diferente de {@link DemandaAprovarRequest}, que é a primeira entrada na
 * coluna. Gera uma nova linha em {@code DemandaStatusKanbanHistorico} com a coluna
 * anterior e a nova.
 */
public record DemandaMoverKanbanRequest(@NotNull(message = "statusKanbanId é obrigatório") Integer statusKanbanId) {
}
