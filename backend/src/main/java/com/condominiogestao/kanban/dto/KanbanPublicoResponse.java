package com.condominiogestao.kanban.dto;

import java.util.List;

/** Resposta de {@code GET /api/kanban-publico/{token}} - endpoint público, sem
 * autenticação nenhuma (ver {@code SecurityConfig}). Só colunas com {@code
 * visivelExternamente = true} e demandas não sigilosas dentro delas (ver {@code
 * KanbanPublicoService}). */
public record KanbanPublicoResponse(String condominioNome, List<ColunaKanbanPublicaResponse> colunas) {
}
