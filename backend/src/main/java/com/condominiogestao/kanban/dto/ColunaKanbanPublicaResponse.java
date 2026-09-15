package com.condominiogestao.kanban.dto;

import java.util.List;

public record ColunaKanbanPublicaResponse(Integer id, String nome, List<CardKanbanPublicoResponse> demandas) {
}
