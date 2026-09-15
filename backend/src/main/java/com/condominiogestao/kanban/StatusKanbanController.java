package com.condominiogestao.kanban;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.kanban.dto.StatusKanbanCreateRequest;
import com.condominiogestao.kanban.dto.StatusKanbanResponse;
import com.condominiogestao.kanban.dto.StatusKanbanUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status-kanban")
@Tag(name = "Status Kanban", description = "Colunas do quadro Kanban, cadastradas por condomínio")
public class StatusKanbanController {

    private final StatusKanbanService service;

    public StatusKanbanController(StatusKanbanService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as colunas de Kanban de um condomínio, na ordem de exibição",
            description = "Morador só recebe as colunas com visivelExternamente=true - funcionário/administrador "
                    + "recebem todas, inclusive as ocultas (pra poder reverter).")
    public List<StatusKanbanResponse> listarPorCondominio(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer condominioId) {
        return service.listarPorCondominio(contexto, condominioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma coluna de Kanban pelo id")
    public StatusKanbanResponse buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova coluna de Kanban",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio.")
    @ApiResponse(responseCode = "403", description = "Só administrador, síndico ou sub-síndico deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public StatusKanbanResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody StatusKanbanCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita nome/ordem/visibilidade/finalístico de uma coluna de Kanban",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio.")
    @ApiResponse(responseCode = "403", description = "Só administrador, síndico ou sub-síndico deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public StatusKanbanResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody StatusKanbanUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma coluna de Kanban",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio. Só permitido enquanto a "
                    + "coluna não tiver nenhuma demanda nela no momento.")
    @ApiResponse(responseCode = "403", description = "Só administrador, síndico ou sub-síndico deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A coluna ainda tem demanda(s) nela",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void excluir(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        service.excluir(contexto, id);
    }
}
