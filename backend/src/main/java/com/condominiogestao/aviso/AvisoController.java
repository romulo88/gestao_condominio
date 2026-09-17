package com.condominiogestao.aviso;

import com.condominiogestao.aviso.dto.AvisoCreateRequest;
import com.condominiogestao.aviso.dto.AvisoResponse;
import com.condominiogestao.aviso.dto.AvisoUpdateRequest;
import com.condominiogestao.common.ErrorResponse;
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

/**
 * `listarVisiveis` (mural) sempre opera no condomínio do token - é "meus avisos".
 * Os endpoints de gestão recebem o condomínio explicitamente (`condominioId` na query ou
 * no corpo), porque administrador pode gerenciar o quadro de avisos de um condomínio que
 * não é o dele - a autorização de verdade está no service (`Autorizacao`), não em confiar
 * cegamente no id recebido.
 */
@RestController
@RequestMapping("/api/avisos")
@Tag(name = "Quadro de Avisos", description = "Avisos gerais do condomínio, mantidos pelos funcionários")
public class AvisoController {

    private final AvisoService service;

    public AvisoController(AvisoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os avisos visíveis agora (ativos e não expirados) no condomínio do contexto",
            description = "É o que deve aparecer pra qualquer pessoa (funcionário ou morador) ao logar.")
    public List<AvisoResponse> listarVisiveis(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listarVisiveis(contexto);
    }

    @GetMapping("/todos")
    @Operation(summary = "Lista todos os avisos de um condomínio (qualquer situação/expiração)",
            description = "Visão de gestão do quadro de avisos - administrador (qualquer condomínio) ou "
                    + "funcionário desse condomínio (qualquer perfil).")
    @ApiResponse(responseCode = "403", description = "Só administrador ou funcionário desse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<AvisoResponse> listarTodos(
            @AuthenticationPrincipal ContextoAutenticado contexto, @RequestParam Integer condominioId) {
        return service.listarTodos(contexto, condominioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um aviso pelo id, dentro do condomínio do contexto")
    public AvisoResponse buscarPorId(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.buscarPorId(contexto, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um aviso - só funcionário do condomínio informado",
            description = "funcionarioId (autor) vem do token, não do corpo. Administrador não pode criar "
                    + "(não é funcionário de nenhum condomínio a não ser que também tenha esse papel).")
    @ApiResponse(responseCode = "403", description = "Só funcionário deste condomínio pode criar aviso",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AvisoResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody AvisoCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige a descrição/expiração de um aviso já existente",
            description = "Autor e condomínio não mudam por essa tela. Administrador (qualquer condomínio) ou "
                    + "funcionário desse condomínio (qualquer perfil) - não exige ser o autor original.")
    @ApiResponse(responseCode = "403", description = "Só administrador ou funcionário desse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AvisoResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody AvisoUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativa um aviso antes da data de expiração",
            description = "Ex: piscina foi liberada antes do prazo que constava no aviso. Administrador "
                    + "(qualquer condomínio) ou funcionário desse condomínio (qualquer perfil).")
    @ApiResponse(responseCode = "403", description = "Só administrador ou funcionário desse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AvisoResponse desativar(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.desativar(contexto, id);
    }

    @PatchMapping("/{id}/fixar-no-topo")
    @Operation(summary = "Fixa um aviso no topo do quadro (ex: telefones da administração)",
            description = "Desfixa automaticamente qualquer outro aviso do mesmo condomínio que já estivesse "
                    + "fixado - só um por vez. Administrador (qualquer condomínio) ou funcionário desse "
                    + "condomínio (qualquer perfil).")
    @ApiResponse(responseCode = "403", description = "Só administrador ou funcionário desse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AvisoResponse fixarNoTopo(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.fixarNoTopo(contexto, id);
    }

    @PatchMapping("/{id}/desfixar-no-topo")
    @Operation(summary = "Remove o destaque de topo de um aviso",
            description = "Administrador (qualquer condomínio) ou funcionário desse condomínio (qualquer perfil).")
    @ApiResponse(responseCode = "403", description = "Só administrador ou funcionário desse condomínio",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AvisoResponse desfixarNoTopo(@AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.desfixarNoTopo(contexto, id);
    }
}
