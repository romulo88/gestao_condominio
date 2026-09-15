package com.condominiogestao.administrador;

import com.condominiogestao.administrador.dto.AdministradorCreateRequest;
import com.condominiogestao.administrador.dto.AdministradorResponse;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administradores")
@Tag(name = "Administradores", description = "Papel global (sem condomínio associado) - opera o sistema como um todo")
public class AdministradorController {

    private final AdministradorService service;

    public AdministradorController(AdministradorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos os administradores")
    public List<AdministradorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um administrador pelo id")
    public AdministradorResponse buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo administrador",
            description = "Só quem já está logado como administrador pode criar outro. O primeiro "
                    + "administrador do sistema é inserido direto no banco (ver HANDOFF.md).")
    public AdministradorResponse criar(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody AdministradorCreateRequest request) {
        return service.criar(contexto, request);
    }
}
