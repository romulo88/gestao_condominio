package com.condominiogestao.mensagemprivada;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaDocumentoResponse;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.storage.ArquivoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** Fotos anexadas a uma mensagem privada - ver {@link MensagemPrivadaDocumentoService}. */
@RestController
@RequestMapping("/api/mensagem-privada-documentos")
@Tag(name = "Mensagem privada documentos", description = "Fotos anexadas a uma mensagem privada")
public class MensagemPrivadaDocumentoController {

    private final MensagemPrivadaDocumentoService service;
    private final ArquivoStorageService arquivoStorageService;

    public MensagemPrivadaDocumentoController(
            MensagemPrivadaDocumentoService service, ArquivoStorageService arquivoStorageService) {
        this.service = service;
        this.arquivoStorageService = arquivoStorageService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma foto anexada a uma mensagem privada",
            description = "Só imagem (jpeg/png/webp/gif). Limite de tamanho e de quantidade por mensagem são "
                    + "parâmetros configuráveis (ver /api/parametros: tamanhoMaximoFotoMb, mensagemPrivadaMaximoFotos). "
                    + "Só o autor da mensagem pode anexar.")
    @ApiResponse(responseCode = "400", description = "Não é imagem, maior que o limite de tamanho, ou arquivo vazio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Só o autor da mensagem pode anexar",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A mensagem já tem o máximo de fotos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MensagemPrivadaDocumentoResponse upload(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @RequestParam Integer mensagemId,
            @RequestParam MultipartFile arquivo) {
        return service.upload(contexto, mensagemId, arquivo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma foto (do storage e do banco)",
            description = "Só o autor da mensagem pode remover.")
    @ApiResponse(responseCode = "403", description = "Só o autor da mensagem pode remover",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void remover(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        service.remover(contexto, id);
    }

    @GetMapping("/{id}/arquivo")
    @Operation(summary = "Serve a foto direto",
            description = "Mesma visibilidade da conversa (só participante). Aceita o token JWT via query string "
                    + "(?token=...) além do header Authorization.")
    @ApiResponse(responseCode = "403", description = "Você não participa dessa conversa",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<InputStreamResource> baixar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        MensagemPrivadaDocumento documento = service.buscarParaBaixar(contexto, id);
        return arquivoStorageService.baixar(documento.getUrl(), documento.getTipoMime(), documento.getTamanhoBytes());
    }
}
