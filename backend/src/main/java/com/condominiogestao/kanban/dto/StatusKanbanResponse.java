package com.condominiogestao.kanban.dto;

import com.condominiogestao.kanban.StatusKanban;
import java.time.LocalDateTime;

public record StatusKanbanResponse(
        Integer id,
        Integer condominioId,
        String nome,
        Integer ordem,
        /** false = coluna (e as demandas nela) oculta pro morador no Kanban - ver
         * {@code DemandaService.listar} e {@code StatusKanbanService.listarPorCondominio}. */
        boolean visivelExternamente,
        /** true = situação terminal do fluxo - demanda nessa coluna pode ser arquivada no
         * card (ver {@code DemandaService.arquivar}). */
        boolean finalistico,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static StatusKanbanResponse from(StatusKanban statusKanban) {
        return new StatusKanbanResponse(
                statusKanban.getId(),
                statusKanban.getCondominio().getId(),
                statusKanban.getNome(),
                statusKanban.getOrdem(),
                statusKanban.isVisivelExternamente(),
                statusKanban.isFinalistico(),
                statusKanban.getCreatedAt(),
                statusKanban.getUpdatedAt());
    }
}
