package com.condominiogestao.espacocomum;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.espacocomum.dto.EspacoComumCreateRequest;
import com.condominiogestao.espacocomum.dto.EspacoComumResponse;
import com.condominiogestao.espacocomum.dto.EspacoComumUpdateRequest;
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

/** Funcionário do condomínio (qualquer perfil), morador (só leitura) ou administrador -
 * ver {@link EspacoComumService}. */
@RestController
@RequestMapping("/api/espacos-comuns")
@Tag(name = "Espaços Comuns", description = "Espaços de lazer do condomínio (aba Espaços de Lazer / local do Evento)")
public class EspacoComumController {

    private final EspacoComumService service;

    public EspacoComumController(EspacoComumService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os espaços de lazer de um condomínio",
            description = "Administrador ou funcionário do condomínio veem todos (ativos ou não); morador "
                    + "(escolhendo o local de um Evento) só recebe os ativos.")
    @ApiResponse(responseCode = "403", description = "Só administrador, funcionário ou morador deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<EspacoComumResponse> listarPorCondominio(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer condominioId) {
        return service.listarPorCondominio(contexto, condominioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo espaço de lazer num condomínio",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um espaço com esse nome nesse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EspacoComumResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody EspacoComumCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige nome e/ou inativa/reativa um espaço de lazer",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe um espaço com esse nome nesse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EspacoComumResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody EspacoComumUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }
}
