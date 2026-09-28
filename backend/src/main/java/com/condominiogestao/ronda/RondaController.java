package com.condominiogestao.ronda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.ronda.dto.RondaDetalheResponse;
import com.condominiogestao.ronda.dto.RondaFinalizarRequest;
import com.condominiogestao.ronda.dto.RondaPontoRequest;
import com.condominiogestao.ronda.dto.RondaResponse;
import com.condominiogestao.ronda.dto.RondaResumoResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sempre opera no condomínio do próprio contexto - ver {@link RondaService}. */
@RestController
@RequestMapping("/api/rondas")
@Tag(name = "Rondas", description = "Controle de rondas dos funcionários de perfil rondista")
public class RondaController {

    private final RondaService service;

    public RondaController(RondaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rondista inicia uma ronda no condomínio do contexto",
            description = "Só perfil rondista. Cada rondista só pode ter uma ronda em andamento por vez - "
                    + "várias rondas de rondistas diferentes do mesmo condomínio podem estar em andamento ao mesmo "
                    + "tempo, sem conflito.")
    @ApiResponse(responseCode = "403", description = "Só rondista pode fazer isso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe uma ronda em andamento desse rondista",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RondaResponse iniciar(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.iniciar(contexto);
    }

    @GetMapping("/ativa")
    @Operation(summary = "Ronda em andamento do rondista logado",
            description = "Usado ao abrir a tela de ronda pra retomar o estado se a página recarregar no meio - "
                    + "404 se não houver nenhuma em andamento.")
    @ApiResponse(responseCode = "404", description = "Nenhuma ronda em andamento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RondaResponse buscarAtiva(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.buscarAtiva(contexto);
    }

    @PostMapping("/{id}/pontos")
    @Operation(summary = "Registra pontos de GPS da ronda, em lote",
            description = "Enviado periodicamente pelo cliente (a cada ~20s), não um a um - tolera sinal ruim. "
                    + "Só o próprio dono da ronda, e só enquanto ela estiver em andamento.")
    @ApiResponse(responseCode = "403", description = "Só o próprio rondista pode fazer isso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Essa ronda já foi encerrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void registrarPontos(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody List<@Valid RondaPontoRequest> pontos) {
        service.registrarPontos(contexto, id, pontos);
    }

    @PatchMapping("/{id}/finalizar")
    @Operation(summary = "Finaliza a ronda",
            description = "Autorizado pro próprio dono (encerramento normal) OU por um perfil completo do "
                    + "condomínio (síndico/sub-síndico/encarregado/supervisor) encerrando uma ronda esquecida de "
                    + "outro rondista - o status resultante muda conforme quem chamou (ver RondaService). Quando "
                    + "é o próprio rondista, o corpo com `tipo` (normal | acompanhamento) é obrigatório e "
                    + "`observacao` (máx. 100 caracteres) é opcional; um perfil completo encerrando a ronda de "
                    + "outra pessoa não manda corpo (ignorado se mandar).")
    @ApiResponse(responseCode = "400", description = "Rondista não informou o tipo, ou observação passou de 100 caracteres",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Só o próprio rondista ou um perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Essa ronda já foi encerrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RondaResponse finalizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) RondaFinalizarRequest request) {
        return service.finalizar(contexto, id, request);
    }

    @GetMapping("/pagina")
    @Operation(summary = "Página da listagem de rondas do condomínio (tela Rondas), 15 por padrão",
            description = "Perfil completo (vê tudo), rondista (só as próprias) e morador (todas, sem nome nem "
                    + "observação). Inclui rondas em andamento, não só finalizadas. Filtros opcionais: rondista "
                    + "(`funcionarioId`, ignorado pro rondista), período (início/fim de `iniciadaEm`), `tipo`, "
                    + "`rondaId` e `observacao` (contém, sem diferenciar maiúscula - ignorado pra quem não vê a "
                    + "observação, senão o filtro vazaria o texto).")
    @ApiResponse(responseCode = "403", description = "Só funcionário ou morador deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<RondaResponse> listarPagina(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam(required = false) Integer funcionarioId,
            @RequestParam(required = false) LocalDateTime inicio,
            @RequestParam(required = false) LocalDateTime fim,
            @RequestParam(required = false) TipoRonda tipo,
            @RequestParam(required = false) Integer rondaId,
            @RequestParam(required = false) String observacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanho) {
        return service.listarPagina(contexto, funcionarioId, inicio, fim, tipo, rondaId, observacao, pagina, tamanho);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo do período filtrado na tela Rondas do síndico",
            description = "Mesmos filtros de GET /pagina - total de rondas, tempo total em ronda e total de "
                    + "demandas abertas durante elas.")
    @ApiResponse(responseCode = "403", description = "Só perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RondaResumoResponse resumo(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam(required = false) Integer funcionarioId,
            @RequestParam(required = false) LocalDateTime inicio,
            @RequestParam(required = false) LocalDateTime fim,
            @RequestParam(required = false) TipoRonda tipo,
            @RequestParam(required = false) Integer rondaId,
            @RequestParam(required = false) String observacao) {
        return service.resumo(contexto, funcionarioId, inicio, fim, tipo, rondaId, observacao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma ronda, com o trajeto inteiro (pro mapa)",
            description = "Autorizado pro próprio dono da ronda ou por um perfil completo do condomínio.")
    @ApiResponse(responseCode = "403", description = "Só o próprio rondista ou um perfil completo deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RondaDetalheResponse buscarDetalhe(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.buscarDetalhe(contexto, id);
    }
}
