package com.condominiogestao.demanda;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.demanda.dto.DemandaDocumentoResponse;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.storage.ArquivoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Anexos de imagem/vídeo numa demanda (item 4.9) - ver {@link DemandaDocumentoService}. */
@RestController
@RequestMapping("/api/demanda-documentos")
@Tag(name = "Demanda documentos", description = "Anexos de imagem/vídeo numa demanda")
public class DemandaDocumentoController {

    private final DemandaDocumentoService service;
    private final ArquivoStorageService arquivoStorageService;

    public DemandaDocumentoController(DemandaDocumentoService service, ArquivoStorageService arquivoStorageService) {
        this.service = service;
        this.arquivoStorageService = arquivoStorageService;
    }

    @GetMapping
    @Operation(summary = "Lista os anexos de uma demanda",
            description = "URL de cada anexo é assinada, expira em 15min. Qualquer funcionário ou morador do "
                    + "condomínio vê (respeitando sigilo) - mesma visibilidade do quadro Kanban.")
    @ApiResponse(responseCode = "403", description = "Não tem acesso a essa demanda (outro condomínio, ou sigilosa)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DemandaDocumentoResponse> listar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer demandaId) {
        return service.listar(contexto, demandaId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma imagem ou vídeo como anexo de uma demanda",
            description = "Imagem (jpeg/png/webp/gif) ou vídeo (mp4/webm/mov) - qualquer outro tipo dá 400. Limite "
                    + "de tamanho e de quantidade por demanda são parâmetros configuráveis (ver /api/parametros: "
                    + "tamanhoMaximoFotoMb, tamanhoMaximoVideoMb, maximoFotos, maximoVideos) - tamanho maior dá 400, "
                    + "quantidade acima do limite dá 409.")
    @ApiResponse(responseCode = "400", description = "Não é imagem/vídeo, maior que o limite de tamanho, ou arquivo vazio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Só funcionário do condomínio ou quem abriu a demanda",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A demanda já tem o máximo de fotos ou de vídeo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DemandaDocumentoResponse upload(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam Integer demandaId,
            @RequestParam MultipartFile arquivo) {
        return service.upload(contexto, demandaId, arquivo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um anexo (do storage e do banco)",
            description = "Remoção física de verdade, diferente do resto do sistema - é só um arquivo, não um registro de auditoria.")
    @ApiResponse(responseCode = "403", description = "Só funcionário do condomínio ou quem abriu a demanda",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void remover(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        service.remover(contexto, id);
    }

    @GetMapping("/{id}/arquivo")
    @Operation(summary = "Serve o anexo (imagem ou vídeo) direto (não é mais link assinado do MinIO)",
            description = "Mesma visibilidade de listar(). Aceita o token JWT via query string (?token=...) além "
                    + "do header Authorization - tag <img>/<video> não manda header, ver JwtAuthenticationFilter.")
    @ApiResponse(responseCode = "403", description = "Não tem acesso a essa demanda (outro condomínio, ou sigilosa)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<InputStreamResource> baixar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        DemandaDocumento documento = service.buscarParaBaixar(contexto, id);
        return arquivoStorageService.baixar(documento.getUrl(), documento.getTipoMime(), documento.getTamanhoBytes());
    }
}
