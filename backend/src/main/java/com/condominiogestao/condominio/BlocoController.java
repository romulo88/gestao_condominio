package com.condominiogestao.condominio;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.condominio.dto.BlocoCreateRequest;
import com.condominiogestao.condominio.dto.BlocoResponse;
import com.condominiogestao.condominio.dto.BlocoUpdateRequest;
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

@RestController
@RequestMapping("/api/blocos")
@Tag(name = "Blocos", description = "Blocos de condomínios do tipo apartamento (item 1.1)")
public class BlocoController {

    private final BlocoService service;

    public BlocoController(BlocoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os blocos de um condomínio")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<BlocoResponse> listarPorCondominio(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer condominioId) {
        return service.listarPorCondominio(contexto, condominioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um bloco pelo id")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BlocoResponse buscarPorId(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.buscarPorId(contexto, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo bloco",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BlocoResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody BlocoCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige o nome de um bloco já existente",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BlocoResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody BlocoUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }
}
