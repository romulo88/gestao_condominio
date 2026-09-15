package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.CandidatoResponsavelResponse;
import com.condominiogestao.demanda.dto.DemandaResponsavelAtribuirRequest;
import com.condominiogestao.demanda.dto.DemandaResponsavelResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Atribuição de responsáveis pela demanda (pode ter mais de um funcionário) - ver
 * {@link DemandaResponsavelService}. */
@RestController
@RequestMapping("/api/demandas/{demandaId}/responsaveis")
@Tag(name = "Responsáveis pela Demanda", description = "Funcionários atribuídos como responsáveis por uma demanda")
public class DemandaResponsavelController {

    private final DemandaResponsavelService service;

    public DemandaResponsavelController(DemandaResponsavelService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os funcionários atribuídos como responsáveis por uma demanda")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaResponsavelResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer demandaId) {
        return service.listar(contexto, demandaId);
    }

    @GetMapping("/candidatos")
    @Operation(summary = "Lista os funcionários ativos do condomínio pra escolher quem atribuir",
            description = "Nome + CPF de cada um - alimenta a combo de busca da tela, em vez de precisar decorar o CPF.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<CandidatoResponsavelResponse> listarCandidatos(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer demandaId) {
        return service.listarCandidatos(contexto, demandaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Atribui um funcionário como responsável pela demanda, pelo CPF")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Nenhum funcionário com esse CPF",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "400", description = "Funcionário sem vínculo ativo com o condomínio dessa demanda",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Esse funcionário já está atribuído",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponsavelResponse atribuir(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer demandaId,
            @Valid @RequestBody DemandaResponsavelAtribuirRequest request) {
        return service.atribuir(contexto, demandaId, request);
    }

    @DeleteMapping("/{atribuicaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um funcionário da lista de responsáveis pela demanda")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void remover(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer demandaId,
            @PathVariable Integer atribuicaoId) {
        service.remover(contexto, demandaId, atribuicaoId);
    }
}
