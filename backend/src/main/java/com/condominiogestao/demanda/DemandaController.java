package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.DemandaAprovarRequest;
import com.condominiogestao.demanda.dto.DemandaCreateRequest;
import com.condominiogestao.demanda.dto.DemandaMoverKanbanRequest;
import com.condominiogestao.demanda.dto.DemandaMudancaStatusResponse;
import com.condominiogestao.demanda.dto.DemandaPaginaResponse;
import com.condominiogestao.demanda.dto.DemandaReprovarRequest;
import com.condominiogestao.demanda.dto.DemandaResponse;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sempre opera no condomínio do próprio contexto - ver {@link DemandaService}. */
@RestController
@RequestMapping("/api/demandas")
@Tag(name = "Demandas", description = "Pedidos de morador/funcionário ao condomínio")
public class DemandaController {

    private final DemandaService service;

    public DemandaController(DemandaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as demandas do condomínio do contexto",
            description = "Por padrão, funcionário vê todas do condomínio e morador vê só as que ele mesmo abriu "
                    + "(uso: tela /demandas, acompanhamento pessoal). Com todas=true, morador também vê todas do "
                    + "condomínio, igual funcionário (uso: quadro Kanban, visão geral do condomínio) - em ambos os "
                    + "casos, demanda sigilosa continua restrita a quem tem direito de vê-la (ver DemandaService).")
    public List<DemandaResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam(defaultValue = "false") boolean todas) {
        return service.listar(contexto, todas);
    }

    @GetMapping("/pagina")
    @Operation(summary = "Página da listagem pessoal de demandas (tela /demandas), 20 por padrão",
            description = "Pedido do Romulo: paginar a listagem em 20 registros. Mesma visibilidade de "
                    + "`GET /api/demandas` sem `todas` (funcionário vê todas do condomínio, morador só as "
                    + "próprias) - o quadro Kanban continua usando o endpoint de sempre, sem paginação, "
                    + "porque precisa da lista inteira pra distribuir nas colunas. `status` aceita um valor de "
                    + "`DemandaStatusAprovacao` OU `kanban:<id>` (situação no Kanban, mesma convenção da tela); "
                    + "`notaNaoLida`/`etapaVencida` ignoram `status` quando marcados, igual o filtro da tela "
                    + "já fazia client-side. `meuResponsavel` (pedido do Romulo) filtra só as demandas em que "
                    + "quem está logado está marcado como responsável.")
    public DemandaPaginaResponse listarPagina(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean notaNaoLida,
            @RequestParam(defaultValue = "false") boolean etapaVencida,
            @RequestParam(defaultValue = "false") boolean meuResponsavel,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listarPagina(contexto, busca, status, notaNaoLida, etapaVencida, meuResponsavel, pagina, tamanho);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma demanda no condomínio do contexto",
            description = "condominioId e solicitante vêm do token - morador ou funcionário logado.")
    public DemandaResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody DemandaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}/aprovar")
    @Operation(summary = "Aprova a demanda, com ou sem Kanban",
            description = "Só funcionário do condomínio da demanda, e só enquanto ela estiver pendente. Informe "
                    + "`statusKanbanId` pra já mandar pra uma coluna, OU `justificativa` pra aprovar sem passar "
                    + "pelo Kanban (mesmo espírito de reprovar) - exatamente um dos dois.")
    @ApiResponse(responseCode = "400", description = "Nem statusKanbanId nem justificativa foram informados",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Demanda já foi decidida antes",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse aprovar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody DemandaAprovarRequest request) {
        return service.aprovar(contexto, id, request);
    }

    @PatchMapping("/{id}/reprovar")
    @Operation(summary = "Reprova a demanda com uma justificativa, sem passar pelo Kanban",
            description = "Uso: demanda que não vai precisar de etapas (já existe outra igual, resolvida na hora, etc).")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Demanda já foi decidida antes",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse reprovar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody DemandaReprovarRequest request) {
        return service.reprovar(contexto, id, request);
    }

    @PatchMapping("/{id}/mover-kanban")
    @Operation(summary = "Move o card de uma demanda já aprovada pra outra coluna do Kanban",
            description = "Uso: arrastar o card no quadro. Só funcionário do condomínio, e só demanda já aprovada.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Demanda ainda não foi aprovada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse moverKanban(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody DemandaMoverKanbanRequest request) {
        return service.moverKanban(contexto, id, request);
    }

    @PatchMapping("/{id}/arquivar")
    @Operation(summary = "Arquiva uma demanda que está numa coluna finalística do Kanban",
            description = "Pedido do Romulo: botão no card. Só funcionário do condomínio, e só demanda numa coluna "
                    + "marcada como finalística. Não apaga nada - só marca arquivada=true.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Demanda não está numa coluna finalística",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse arquivar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.arquivar(contexto, id);
    }

    @GetMapping("/mudancas-status")
    @Operation(summary = "Alerta de login do morador: mudanças de status nas demandas dele desde uma data",
            description = "Só morador. Junta aprovação/recusa e movimentação de coluna no Kanban das demandas que "
                    + "o próprio morador abriu E das que ele marcou 'Acompanhar' (ver POST .../acompanhar), desde "
                    + "`desde` (ISO, ex: 2026-09-01T10:00:00) - pensado pra passar o `ultimoLoginAnterior` que veio "
                    + "no login. Cada evento vem com `origem` ('propria' ou 'acompanhada') pra distinguir as duas.")
    @ApiResponse(responseCode = "403", description = "Só morador tem esse alerta",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaMudancaStatusResponse> mudancasStatus(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam LocalDateTime desde) {
        return service.mudancasStatus(contexto, desde);
    }

    @PostMapping("/{id}/acompanhar")
    @Operation(summary = "Morador marca 'Acompanhar' numa demanda que ele não abriu",
            description = "Idempotente - marcar de novo não duplica. A partir daí, mudanças de status dessa "
                    + "demanda entram no alerta de login do morador junto das próprias (ver GET .../mudancas-status). "
                    + "Exige a mesma visibilidade normal da demanda (respeita sigilo).")
    @ApiResponse(responseCode = "403", description = "Só morador, e só demanda que ele tem direito de ver",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Essa demanda é do próprio morador - não tem o que acompanhar",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse acompanhar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.acompanhar(contexto, id);
    }

    @DeleteMapping("/{id}/acompanhar")
    @Operation(summary = "Morador desmarca 'Acompanhar' numa demanda", description = "Idempotente.")
    @ApiResponse(responseCode = "403", description = "Só morador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse deixarDeAcompanhar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.deixarDeAcompanhar(contexto, id);
    }

    @PatchMapping("/{id}/alternar-sigilo")
    @Operation(summary = "Marca/desmarca a demanda como sigilosa (alterna)",
            description = "Único jeito de funcionário 'editar' uma demanda depois de criada - título/descrição "
                    + "continuam fixos. Funciona em qualquer status, qualquer funcionário do condomínio.")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaResponse alternarSigilo(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.alternarSigilo(contexto, id);
    }
}
