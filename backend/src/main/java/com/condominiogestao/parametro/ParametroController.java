package com.condominiogestao.parametro;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.parametro.dto.ParametroCreateRequest;
import com.condominiogestao.parametro.dto.ParametroResponse;
import com.condominiogestao.parametro.dto.ParametroUpdateRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Parâmetros gerais do sistema - 100% administrador-only, ver {@link ParametroService}. */
@RestController
@RequestMapping("/api/parametros")
@Tag(name = "Parâmetros", description = "Valores de regra de negócio ajustáveis pelo administrador, sem deploy")
public class ParametroController {

    private final ParametroService service;

    public ParametroController(ParametroService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos os parâmetros, em ordem alfabética de nome")
    @ApiResponse(responseCode = "403", description = "Só administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<ParametroResponse> listar(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listar(contexto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo parâmetro",
            description = "Cadastrar aqui não tem efeito por si só - só o código que lê esse nome (getInt/etc, "
                    + "ver ParametroService) passa a valer.")
    @ApiResponse(responseCode = "403", description = "Só administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um parâmetro com esse nome",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ParametroResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody ParametroCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige descrição/valor de um parâmetro já existente",
            description = "Nome não é editável - é a chave que o código usa pra ler o valor.")
    @ApiResponse(responseCode = "403", description = "Só administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ParametroResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody ParametroUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um parâmetro",
            description = "Se algum código ainda ler esse nome, volta a valer o default embutido no próprio código.")
    @ApiResponse(responseCode = "403", description = "Só administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void excluir(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        service.excluir(contexto, id);
    }
}
