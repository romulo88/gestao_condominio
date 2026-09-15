package com.condominiogestao.kanban;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.kanban.dto.KanbanPublicoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sem autenticação nenhuma - liberado em {@code SecurityConfig} (`/api/kanban-publico/**`).
 * Pedido do Romulo: link externo do quadro Kanban, pra ver sem precisar estar logado. */
@RestController
@RequestMapping("/api/kanban-publico")
@Tag(name = "Kanban público", description = "Leitura pública (sem login) do quadro Kanban via link compartilhável")
public class KanbanPublicoController {

    private final KanbanPublicoService service;

    public KanbanPublicoController(KanbanPublicoService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Busca o quadro Kanban público de um condomínio pelo token do link",
            description = "Sem autenticação - qualquer um com o link acessa. Só colunas visíveis "
                    + "externamente e demandas não sigilosas; sem dado pessoal (sem nome de "
                    + "solicitante/responsável, sem descrição/notas/etapas) e sem detalhe de card - a "
                    + "tela pública não abre modal nenhum, só mostra os cards na coluna deles.")
    @ApiResponse(responseCode = "404", description = "Link inválido, revogado, ou nunca existiu",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public KanbanPublicoResponse buscar(@PathVariable String token) {
        return service.buscarPorToken(token);
    }
}
