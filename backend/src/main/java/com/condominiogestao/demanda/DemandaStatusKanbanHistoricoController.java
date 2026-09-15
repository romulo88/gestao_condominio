package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.DemandaStatusKanbanHistoricoResponse;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Histórico de transição de coluna do Kanban - ícone de relógio no card. */
@RestController
@RequestMapping("/api/demanda-status-kanban-historico")
@Tag(name = "Histórico de Kanban", description = "Uma linha por transição de coluna, com quando e quem mudou")
public class DemandaStatusKanbanHistoricoController {

    private final DemandaStatusKanbanHistoricoService service;

    public DemandaStatusKanbanHistoricoController(DemandaStatusKanbanHistoricoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista o histórico de transições de coluna de uma demanda, em ordem cronológica")
    @ApiResponse(responseCode = "403", description = "Só quem pode ver a demanda no quadro Kanban",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaStatusKanbanHistoricoResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer demandaId) {
        return service.listarPorDemanda(contexto, demandaId);
    }
}
