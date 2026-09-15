package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.DemandaEtapaCreateRequest;
import com.condominiogestao.demanda.dto.DemandaEtapaResponse;
import com.condominiogestao.demanda.dto.DemandaEtapaUpdateRequest;
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

/** Checklist interno da demanda (item 4.5) - criado/gerenciado só pelo funcionário do
 * condomínio da demanda; morador vê em modo só-leitura (ver {@link DemandaEtapaService}). */
@RestController
@RequestMapping("/api/demanda-etapas")
@Tag(name = "Etapas da Demanda", description = "Checklist interno com prazo, definido pelo funcionário")
public class DemandaEtapaController {

    private final DemandaEtapaService service;

    public DemandaEtapaController(DemandaEtapaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as etapas de uma demanda, na ordem",
            description = "Qualquer funcionário do condomínio ou morador com acesso a essa demanda (sigilo "
                    + "incluso) - morador só lê, não cria/edita/conclui.")
    @ApiResponse(responseCode = "403", description = "Não tem acesso a essa demanda (outro condomínio, ou sigilosa)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaEtapaResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer demandaId) {
        return service.listarPorDemanda(contexto, demandaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma etapa numa demanda")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaEtapaResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody DemandaEtapaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita nome/prazo/ordem de uma etapa")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaEtapaResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody DemandaEtapaUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Marca/desmarca uma etapa como concluída (alterna)")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaEtapaResponse alternarConcluida(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.alternarConcluida(contexto, id);
    }
}
