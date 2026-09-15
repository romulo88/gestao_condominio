package com.condominiogestao.condominio;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.condominio.dto.CondominioCreateRequest;
import com.condominiogestao.condominio.dto.CondominioResponse;
import com.condominiogestao.condominio.dto.CondominioUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.storage.ArquivoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/condominios")
@Tag(name = "Condomínios", description = "Cadastro de condomínios (item 1)")
public class CondominioController {

    private final CondominioService service;
    private final ArquivoStorageService arquivoStorageService;

    public CondominioController(CondominioService service, ArquivoStorageService arquivoStorageService) {
        this.service = service;
        this.arquivoStorageService = arquivoStorageService;
    }

    @GetMapping
    @Operation(summary = "Lista os condomínios",
            description = "Administrador vê todos; funcionário/morador só vê o condomínio do próprio contexto.")
    public List<CondominioResponse> listar(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listar(contexto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um condomínio pelo id")
    @ApiResponse(responseCode = "404", description = "Condomínio não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CondominioResponse buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo condomínio", description = "Só administrador pode cadastrar.")
    @ApiResponse(responseCode = "403", description = "Quem chamou não é administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "400", description = "Erro de validação",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CondominioResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody CondominioCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita um condomínio", description = "Só administrador pode editar.")
    @ApiResponse(responseCode = "403", description = "Quem chamou não é administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Condomínio não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "CNPJ já usado por outro condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CondominioResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody CondominioUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativa um condomínio",
            description = "\"Remover\" na prática é desativar (situacao = inativo) - não existe exclusão "
                    + "física, o mesmo padrão do resto do projeto. Só administrador pode desativar.")
    @ApiResponse(responseCode = "403", description = "Quem chamou não é administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Condomínio não encontrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CondominioResponse desativar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.desativar(contexto, id);
    }

    @PostMapping("/{id}/kanban-publico")
    @Operation(summary = "Gera (ou devolve o já existente) o link público de leitura do Kanban",
            description = "Pedido do Romulo: link externo pra ver o quadro sem precisar estar logado - só "
                    + "mostra colunas visíveis externamente e demandas não sigilosas, sem abrir detalhe de "
                    + "card. Idempotente: chamar de novo com um link já gerado devolve o MESMO token, não "
                    + "troca. Administrador, ou síndico/sub-síndico do próprio condomínio.")
    @ApiResponse(responseCode = "403", description = "Só administrador, síndico ou sub-síndico deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CondominioResponse gerarLinkPublicoKanban(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.gerarLinkPublicoKanban(contexto, id);
    }

    @DeleteMapping("/{id}/kanban-publico")
    @Operation(summary = "Revoga o link público do Kanban",
            description = "Invalida qualquer link já compartilhado na hora. Administrador, ou "
                    + "síndico/sub-síndico do próprio condomínio.")
    public CondominioResponse revogarLinkPublicoKanban(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.revogarLinkPublicoKanban(contexto, id);
    }

    @PostMapping(value = "/{id}/gif", consumes = "multipart/form-data")
    @Operation(summary = "Sobe (ou substitui) o GIF opcional do condomínio",
            description = "Exibido no lugar do título da página no Kanban, quando cadastrado. Só GIF (image/gif) "
                    + "- qualquer outro tipo dá 400. Administrador, ou síndico/sub-síndico do próprio condomínio.")
    @ApiResponse(responseCode = "403", description = "Só administrador, síndico ou sub-síndico deste condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CondominioResponse atualizarGif(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @RequestParam MultipartFile arquivo) {
        return service.atualizarGif(contexto, id, arquivo);
    }

    @DeleteMapping("/{id}/gif")
    @Operation(summary = "Remove o GIF do condomínio",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio.")
    public CondominioResponse removerGif(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.removerGif(contexto, id);
    }

    @GetMapping("/{id}/gif")
    @Operation(summary = "Serve o GIF do condomínio direto (não é mais link assinado do MinIO)",
            description = "Aceita o token JWT via query string (?token=...) além do header Authorization - "
                    + "tag <img> não manda header, ver JwtAuthenticationFilter.")
    public ResponseEntity<InputStreamResource> baixarGif(@PathVariable Integer id) {
        return arquivoStorageService.baixar(service.chaveGif(id), null);
    }
}
