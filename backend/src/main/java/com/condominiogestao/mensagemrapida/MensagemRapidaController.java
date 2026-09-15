package com.condominiogestao.mensagemrapida;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.mensagemrapida.dto.MensagemRapidaCreateRequest;
import com.condominiogestao.mensagemrapida.dto.MensagemRapidaResponse;
import com.condominiogestao.mensagemrapida.dto.MensagemRapidaUpdateRequest;
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
 * ver {@link MensagemRapidaService}. */
@RestController
@RequestMapping("/api/mensagens-rapidas")
@Tag(name = "Mensagens rápidas", description = "Mensagens prontas (positivas/negativas), cadastradas por condomínio")
public class MensagemRapidaController {

    private final MensagemRapidaService service;

    public MensagemRapidaController(MensagemRapidaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as mensagens rápidas ativas de um condomínio")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<MensagemRapidaResponse> listarPorCondominio(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer condominioId) {
        return service.listarPorCondominio(contexto, condominioId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova mensagem rápida num condomínio",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MensagemRapidaResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @Valid @RequestBody MensagemRapidaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige texto/caráter de uma mensagem rápida já existente",
            description = "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MensagemRapidaResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody MensagemRapidaUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @PatchMapping("/{id}/excluir")
    @Operation(summary = "Exclui (soft-delete) uma mensagem rápida",
            description = "Não apaga do banco - só marca situacao = inativo, mesmo espírito de Aviso.desativar. "
                    + "Funcionário (qualquer perfil) do próprio condomínio, ou administrador em qualquer um.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio ou administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MensagemRapidaResponse excluir(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.excluir(contexto, id);
    }
}
