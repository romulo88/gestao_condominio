package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.DemandaEtiquetaVincularRequest;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Anexar/remover etiqueta de uma demanda (item 4.6) - só funcionário do condomínio. */
@RestController
@RequestMapping("/api/demanda-etiquetas")
@Tag(name = "Etiquetas da Demanda", description = "Vínculo N:N entre demanda e etiqueta - só funcionário")
public class DemandaEtiquetaController {

    private final DemandaEtiquetaService service;

    public DemandaEtiquetaController(DemandaEtiquetaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Anexa uma etiqueta numa demanda")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Essa etiqueta já está nessa demanda",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EtiquetaResponse vincular(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody DemandaEtiquetaVincularRequest request) {
        return service.vincular(contexto, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma etiqueta de uma demanda")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void desvincular(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam Integer demandaId,
            @RequestParam Integer etiquetaId) {
        service.desvincular(contexto, demandaId, etiquetaId);
    }
}
