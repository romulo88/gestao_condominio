package com.condominiogestao.evento;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.evento.dto.EventoCreateRequest;
import com.condominiogestao.evento.dto.EventoResponse;
import com.condominiogestao.evento.dto.EventoUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Sempre opera no condomínio do próprio contexto - ver {@link EventoService}. */
@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Cadastro de eventos (festa/visita) pelo morador, calendário e liberação pela portaria")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Morador cadastra um evento (festa/visita) com veículos e pessoas esperados",
            description = "Só morador. `espacoComumId` omitido = local é a própria unidade do morador.")
    @ApiResponse(responseCode = "400", description = "Data no passado, ou nenhuma pessoa informada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Só morador pode fazer isso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody EventoCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Morador edita o próprio evento",
            description = "Só o próprio morador que cadastrou, e só antes da data do evento passar. Sincroniza "
                    + "veículos/pessoas item a item (não substitui tudo) - item com `id` é atualizado, sem `id` é "
                    + "criado, e um existente que não vier na lista é removido; item já liberado pela portaria é "
                    + "sempre preservado intacto, mesmo que não venha na lista ou venha com dados diferentes. Local "
                    + "(`espacoComumId`) e `data` só podem mudar se a data ATUAL (antes desta edição) ainda não é "
                    + "hoje - no dia do evento, os dois precisam vir iguais ao que já está salvo.")
    @ApiResponse(responseCode = "400", description = "Data no passado, nenhuma pessoa (no total), ou tentativa de "
            + "mudar data/local no dia do evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Só o próprio morador que cadastrou pode fazer isso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "id de veículo/pessoa que não pertence a este evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Evento já aconteceu",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody EventoUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Morador apaga o próprio evento",
            description = "Só o próprio morador que cadastrou, e só enquanto o evento ainda não ocorreu e nenhum "
                    + "veículo/pessoa já foi liberado pela portaria - mesma autorização de PATCH.")
    @ApiResponse(responseCode = "403", description = "Só o próprio morador que cadastrou pode fazer isso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Evento já aconteceu, ou já tem item liberado pela portaria",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void excluir(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        service.excluir(contexto, id);
    }

    @GetMapping("/meus")
    @Operation(summary = "Lista os próprios eventos do morador logado, mais recente primeiro")
    @ApiResponse(responseCode = "403", description = "Só morador pode fazer isso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<EventoResponse> listarMeusEventos(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listarMeusEventos(contexto);
    }

    @GetMapping("/pagina")
    @Operation(summary = "Página do calendário de eventos do condomínio (tela Portaria), 20 por padrão",
            description = "Só perfil `porteiro` ou perfil completo (síndico/sub-síndico/encarregado/supervisor). "
                    + "Filtros opcionais: `dataInicio`/`dataFim` (dia, inclusive) e `espacoComumId`.")
    @ApiResponse(responseCode = "403", description = "Só porteiro ou perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<EventoResponse> listarPagina(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Integer espacoComumId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listarPagina(contexto, dataInicio, dataFim, espacoComumId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um evento, com veículos e pessoas",
            description = "Autorizado pro próprio morador dono do evento, ou por porteiro/perfil completo do condomínio.")
    @ApiResponse(responseCode = "403", description = "Só o próprio morador, o porteiro ou um perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse buscarDetalhe(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.buscarDetalhe(contexto, id);
    }

    @PatchMapping("/{id}/veiculos/{veiculoId}/liberar")
    @Operation(summary = "Libera (ou desfaz a liberação de) um veículo do evento",
            description = "Toggle - chamar de novo desfaz, corrige engano sem endpoint separado. Só porteiro ou "
                    + "perfil completo deste condomínio.")
    @ApiResponse(responseCode = "403", description = "Só porteiro ou perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse liberarVeiculo(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id, @PathVariable Integer veiculoId) {
        return service.liberarVeiculo(contexto, id, veiculoId);
    }

    @PatchMapping("/{id}/pessoas/{pessoaId}/liberar")
    @Operation(summary = "Libera (ou desfaz a liberação de) uma pessoa do evento",
            description = "Toggle - chamar de novo desfaz, corrige engano sem endpoint separado. Só porteiro ou "
                    + "perfil completo deste condomínio.")
    @ApiResponse(responseCode = "403", description = "Só porteiro ou perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse liberarPessoa(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id, @PathVariable Integer pessoaId) {
        return service.liberarPessoa(contexto, id, pessoaId);
    }
}
