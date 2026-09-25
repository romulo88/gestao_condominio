package com.condominiogestao.morador;

import com.condominiogestao.morador.dto.MoradorCreateRequest;
import com.condominiogestao.morador.dto.MoradorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moradores")
@Tag(name = "Moradores", description = "Cadastro de moradores (item 3) - pessoa, sem vínculo de condomínio/unidade ainda. Item 5.3: só funcionário cadastra (regra a aplicar quando a autenticação existir)")
public class MoradorController {

    private final MoradorService service;

    public MoradorController(MoradorService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista todos os moradores")
    public List<MoradorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um morador pelo id")
    public MoradorResponse buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/buscar-por-email")
    @Operation(summary = "Busca um morador pelo e-mail",
            description = "404 se o e-mail não tem papel de morador ainda (mesmo que já exista como pessoa).")
    public MoradorResponse buscarPorEmail(@RequestParam String email) {
        return service.buscarPorEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo morador (sem senha - ver fluxo de autenticação)")
    public MoradorResponse criar(@Valid @RequestBody MoradorCreateRequest request) {
        return service.criar(request);
    }
}
