package com.condominiogestao.demanda.dto;

/**
 * Aprova a demanda. {@code statusKanbanId} já manda pra uma coluna do Kanban do próprio
 * condomínio (normalmente a primeira, tipo "Fila") - o funcionário escolhe qual, entre as
 * colunas já cadastradas em {@code StatusKanban}. Pedido do Romulo: também dá pra aprovar
 * SEM Kanban - omite {@code statusKanbanId} e informa {@code justificativa} em vez disso
 * (mesmo espírito de {@code DemandaReprovarRequest}). Exatamente um dos dois é exigido -
 * checagem em tempo de execução no service, não dá pra expressar "um OU outro" só com
 * anotação de bean validation.
 */
public record DemandaAprovarRequest(Integer statusKanbanId, String justificativa) {
}
