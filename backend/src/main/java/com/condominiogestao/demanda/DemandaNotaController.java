package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.DemandaNotaCreateRequest;
import com.condominiogestao.demanda.dto.DemandaNotaResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Notas de uma demanda (pedido do Romulo) - ver {@link DemandaNotaService}. */
@RestController
@RequestMapping("/api/demanda-notas")
@Tag(name = "Demanda notas", description = "Perguntas do morador sobre o andamento e respostas do funcionário")
public class DemandaNotaController {

    private final DemandaNotaService service;

    public DemandaNotaController(DemandaNotaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as notas de uma demanda, mais antiga primeiro",
            description = "Qualquer funcionário ou morador do condomínio vê (respeitando sigilo) - mesma "
                    + "visibilidade do quadro Kanban.")
    @ApiResponse(responseCode = "403", description = "Não tem acesso a essa demanda (outro condomínio, ou sigilosa)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaNotaResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer demandaId) {
        return service.listar(contexto, demandaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nota (pergunta) ou uma resposta a outra nota",
            description = "Informe notaPaiId pra responder a uma nota já existente - some com a mesma "
                    + "visibilidade da leitura. Só até a demanda ser arquivada. Quando quem responde é "
                    + "funcionário, a nota-pai é marcada como lida automaticamente.")
    @ApiResponse(responseCode = "403", description = "Não tem acesso a essa demanda",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Demanda arquivada - não aceita nota nova",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaNotaResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody DemandaNotaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}/marcar-lida")
    @Operation(summary = "Marca uma nota como lida",
            description = "Só funcionário do condomínio da demanda. Idempotente - marcar de novo não muda nada.")
    @ApiResponse(responseCode = "403", description = "Só funcionário pode marcar como lida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaNotaResponse marcarLida(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.marcarLida(contexto, id);
    }
}
