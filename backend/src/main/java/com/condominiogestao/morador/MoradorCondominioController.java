package com.condominiogestao.morador;

import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.morador.dto.MoradorCondominioCreateRequest;
import com.condominiogestao.morador.dto.MoradorCondominioResponse;
import com.condominiogestao.morador.dto.MoradorCondominioResumoResponse;
import com.condominiogestao.morador.dto.MoradorCondominioUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/moradores-condominios")
@Tag(name = "Vínculos Morador-Condomínio", description = "Um morador pode residir em vários condomínios/unidades, com situação própria em cada uma")
public class MoradorCondominioController {

    private final MoradorCondominioService service;

    public MoradorCondominioController(MoradorCondominioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista vínculos, filtrando por condomínio ou por morador (opcional)")
    public List<MoradorCondominioResponse> listar(
            @RequestParam(required = false) Integer condominioId,
            @RequestParam(required = false) Integer moradorId) {
        return service.listar(condominioId, moradorId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um vínculo pelo id")
    public MoradorCondominioResponse buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/pagina")
    @Operation(summary = "Página da listagem de moradores de um condomínio, com nome/CPF/e-mail já embutidos",
            description = "Pedido do Romulo: 15 registros por página por padrão, pra não listar todos de uma vez. "
                    + "`busca` (opcional) filtra por nome (contém) ou pelos dígitos do CPF (contém).")
    public PaginaResponse<MoradorCondominioResumoResponse> listarPagina(
            @RequestParam Integer condominioId,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanho) {
        return service.listarPaginaPorCondominio(condominioId, busca, pagina, tamanho);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria o vínculo de um morador com um condomínio/unidade",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio.")
    public MoradorCondominioResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @Valid @RequestBody MoradorCondominioCreateRequest request) {
        return service.criar(contexto, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Corrige bloco/unidade de um vínculo já existente",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio - mesma regra de criar.")
    public MoradorCondominioResponse atualizar(
            @AuthenticationPrincipal ContextoAutenticado contexto,
            @PathVariable Integer id,
            @Valid @RequestBody MoradorCondominioUpdateRequest request) {
        return service.atualizar(contexto, id, request);
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativa o vínculo (soft-delete - nunca apaga a linha)",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio - mesma regra de criar.")
    public MoradorCondominioResponse desativar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.desativar(contexto, id);
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa um vínculo previamente desativado",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio - mesma regra de criar.")
    public MoradorCondominioResponse ativar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.ativar(contexto, id);
    }

    @PatchMapping("/{id}/zerar-senha")
    @Operation(summary = "Reseta a senha da pessoa por trás do vínculo pra padrão, exigindo troca no próximo login",
            description = "Administrador, ou síndico/sub-síndico do próprio condomínio - mesma regra de criar. "
                    + "Pra quando o morador esqueceu a senha atual (não só nunca trocou a padrão).")
    public MoradorCondominioResponse zerarSenha(
            @AuthenticationPrincipal ContextoAutenticado contexto, @PathVariable Integer id) {
        return service.zerarSenha(contexto, id);
    }
}
