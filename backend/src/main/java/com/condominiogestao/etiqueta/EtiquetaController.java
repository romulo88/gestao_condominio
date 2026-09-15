package com.condominiogestao.etiqueta;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.etiqueta.dto.EtiquetaCreateRequest;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import com.condominiogestao.etiqueta.dto.EtiquetaUpdateRequest;
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

/** Funcionário do condomínio (qualquer perfil) ou administrador (qualquer condomínio) -
 * ver {@link EtiquetaService}. */
@RestController
@RequestMapping("/api/etiquetas")
@Tag(name = "Etiquetas", description = "Etiquetas pra classificar demandas, cadastradas por condomínio")
public class EtiquetaController {

    private final EtiquetaService service;

    public EtiquetaController(EtiquetaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as etiquetas ativas de um condomínio",
            description = "Administrador ou funcionário do condomínio veem todas; morador (pedido do "
                    + "Romulo: filtro de etiqueta no Kanban) só recebe as marcadas visível pro morador.")
    @ApiResponse(responseCode = "403", description = "Só administrador, funcionário ou morador deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<EtiquetaResponse> listarPorCondominio(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer condominioId) {
        return service.listarPorCondominio(contexto, condominioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma etiqueta pelo id")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EtiquetaResponse buscarPorId(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.buscarPorId(contexto, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova etiqueta num condomínio",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EtiquetaResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody EtiquetaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige texto/cor de uma etiqueta já existente",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EtiquetaResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody EtiquetaUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }
}
