package com.condominiogestao.tarefaagendada;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.tarefaagendada.dto.TarefaAgendadaCreateRequest;
import com.condominiogestao.tarefaagendada.dto.TarefaAgendadaResponse;
import com.condominiogestao.tarefaagendada.dto.TarefaAgendadaUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sempre opera no condomínio do próprio contexto - ver {@link TarefaAgendadaService}. */
@RestController
@RequestMapping("/api/tarefas-agendadas")
@Tag(name = "Tarefas agendadas", description = "Lembretes com data, mantidos pelos funcionários do condomínio")
public class TarefaAgendadaController {

    private final TarefaAgendadaService service;

    public TarefaAgendadaController(TarefaAgendadaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as tarefas agendadas do condomínio do contexto",
            description = "Ordenadas pela data mais próxima entre as três (data da tarefa / 1º aviso / 2º aviso), "
                    + "crescente. Só funcionário.")
    @ApiResponse(responseCode = "403", description = "Só funcionário pode ver tarefas agendadas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<TarefaAgendadaResponse> listar(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listar(contexto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma tarefa agendada no condomínio do contexto",
            description = "condominioId e autor vêm do token. Qualquer funcionário do condomínio pode.")
    @ApiResponse(responseCode = "403", description = "Só funcionário pode cadastrar tarefa agendada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TarefaAgendadaResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @Valid @RequestBody TarefaAgendadaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige título/descrição/datas de uma tarefa agendada já existente",
            description = "Qualquer funcionário do condomínio pode, não só quem cadastrou.")
    @ApiResponse(responseCode = "403", description = "Só funcionário pode editar tarefa agendada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TarefaAgendadaResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody TarefaAgendadaUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @PatchMapping("/{id}/excluir")
    @Operation(summary = "Exclui (soft-delete) uma tarefa agendada",
            description = "Não apaga do banco - só marca situacao = inativo. Qualquer funcionário do condomínio pode.")
    @ApiResponse(responseCode = "403", description = "Só funcionário pode excluir tarefa agendada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TarefaAgendadaResponse excluir(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.excluir(contexto, id);
    }
}
