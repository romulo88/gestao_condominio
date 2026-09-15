package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.CandidatoAcessoResponse;
import com.condominiogestao.demanda.dto.DemandaAcessoSigilosoConcederRequest;
import com.condominiogestao.demanda.dto.DemandaAcessoSigilosoResponse;
import com.condominiogestao.demanda.dto.VisualizadorPorRegraResponse;
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

/** Item 4.8 - quem além do solicitante/síndico/sub-síndico/quem marcou pode ver uma
 * demanda sigilosa. Ver {@link DemandaAcessoSigilosoService}. */
@RestController
@RequestMapping("/api/demandas/{demandaId}/acesso-sigiloso")
@Tag(name = "Acesso a Demanda Sigilosa", description = "Quem mais, além do padrão, enxerga uma demanda marcada como sigilosa")
public class DemandaAcessoSigilosoController {

    private final DemandaAcessoSigilosoService service;

    public DemandaAcessoSigilosoController(DemandaAcessoSigilosoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista quem tem acesso extra a uma demanda sigilosa")
    @ApiResponse(responseCode = "403", description = "Só síndico, sub-síndico, ou quem marcou como sigilosa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaAcessoSigilosoResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer demandaId) {
        return service.listar(contexto, demandaId);
    }

    @GetMapping("/por-regra")
    @Operation(summary = "Lista quem já vê essa demanda sigilosa POR REGRA (síndico/sub-síndico do condomínio)",
            description = "Informativo, sem botão de revogar - diferente da concessão explícita do GET principal. "
                    + "Mesma autorização de GET (listar acesso).")
    @ApiResponse(responseCode = "403", description = "Só síndico, sub-síndico, ou quem marcou como sigilosa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<VisualizadorPorRegraResponse> listarPorRegra(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer demandaId) {
        return service.listarVisualizacaoPorRegra(contexto, demandaId);
    }

    @GetMapping("/candidatos")
    @Operation(summary = "Lista as pessoas ativas do condomínio (morador e funcionário) pra escolher quem ganha acesso",
            description = "Nome + unidade (só morador) + CPF de cada uma - alimenta a combo de busca da tela, "
                    + "em vez de precisar decorar o CPF. Mesma autorização de GET (listar acesso).")
    @ApiResponse(responseCode = "403", description = "Só síndico, sub-síndico, ou quem marcou como sigilosa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<CandidatoAcessoResponse> listarCandidatos(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer demandaId) {
        return service.listarCandidatos(contexto, demandaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Concede acesso a uma demanda sigilosa pelo CPF",
            description = "Concede em todos os papéis (morador e/ou funcionário) que a pessoa tiver vínculo ativo com o condomínio.")
    @ApiResponse(responseCode = "403", description = "Só síndico, sub-síndico, ou quem marcou como sigilosa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ninguém com esse CPF tem vínculo ativo com o condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaAcessoSigilosoResponse> conceder(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer demandaId,
            @Valid @RequestBody DemandaAcessoSigilosoConcederRequest request) {
        return service.conceder(contexto, demandaId, request);
    }

    @DeleteMapping("/{acessoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoga um acesso concedido")
    @ApiResponse(responseCode = "403", description = "Só síndico, sub-síndico, ou quem marcou como sigilosa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void revogar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer demandaId,
            @PathVariable Integer acessoId) {
        service.revogar(contexto, demandaId, acessoId);
    }
}
