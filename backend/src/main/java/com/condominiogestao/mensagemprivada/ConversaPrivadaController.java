package com.condominiogestao.mensagemprivada;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.mensagemprivada.dto.CandidatoDestinatarioResponse;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaCreateRequest;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaDetalheResponse;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaResumoResponse;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaCreateRequest;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mensagem privada (pedido do Romulo) - ver {@link ConversaPrivadaService}. */
@RestController
@RequestMapping("/api/conversas-privadas")
@Tag(name = "Mensagem privada", description = "Conversas privadas entre morador/funcionário e funcionário(s) com login")
public class ConversaPrivadaController {

    private final ConversaPrivadaService service;

    public ConversaPrivadaController(ConversaPrivadaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as minhas conversas privadas (paginado, 20 por padrão - mesmo tamanho de /api/demandas/pagina)",
            description = "Autor OU destinatário, conforme o papel de quem está logado - mais recente primeiro. "
                    + "Só quem participa vê (nem síndico por padrão, privacidade estrita).")
    public PaginaResponse<ConversaPrivadaResumoResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listarPagina(contexto, pagina, tamanho);
    }

    @GetMapping("/pendente")
    @Operation(summary = "Existe conversa com mensagem não vista endereçada a mim?",
            description = "Boolean leve pra alimentar o destaque vermelho no ícone do menu, sem carregar a "
                    + "listagem inteira.")
    public boolean existePendencia(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.existePendencia(contexto);
    }

    @GetMapping("/candidatos")
    @Operation(summary = "Funcionários com login do meu condomínio, pra endereçar uma mensagem",
            description = "Só funcionário com perfil preenchido (login ativo) e vínculo/situação ativos.")
    public List<CandidatoDestinatarioResponse> listarCandidatos(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listarCandidatos(contexto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Abre uma conversa (chat completo)",
            description = "Marca como vista por quem está abrindo - some o destaque vermelho.")
    @ApiResponse(responseCode = "403", description = "Você não participa dessa conversa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ConversaPrivadaDetalheResponse buscarDetalhe(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.buscarDetalhe(contexto, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conversa privada nova, com a primeira mensagem",
            description = "Endereçada a 1 ou mais funcionários com login do mesmo condomínio (ex: síndico e "
                    + "sub-síndico ao mesmo tempo).")
    @ApiResponse(responseCode = "400", description = "Destinatário sem login ativo nesse condomínio, ou nenhum destinatário informado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ConversaPrivadaDetalheResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody ConversaPrivadaCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PostMapping("/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma nova mensagem numa conversa já existente",
            description = "Conversa livre - qualquer participante (autor original ou qualquer destinatário) pode escrever.")
    @ApiResponse(responseCode = "403", description = "Você não participa dessa conversa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MensagemPrivadaResponse enviarMensagem(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody MensagemPrivadaCreateRequest request) {
        return service.enviarMensagem(contexto, request);
    }
}
