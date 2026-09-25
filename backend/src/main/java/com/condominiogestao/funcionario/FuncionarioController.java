package com.condominiogestao.funcionario;

import com.condominiogestao.funcionario.dto.FuncionarioCreateRequest;
import com.condominiogestao.funcionario.dto.FuncionarioResponse;
import com.condominiogestao.storage.ArquivoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/funcionarios")
@Tag(name = "Funcionários", description = "Cadastro de funcionários (item 2) - pessoa, sem vínculo de condomínio ainda")
public class FuncionarioController {

    private final FuncionarioService service;
    private final ArquivoStorageService arquivoStorageService;

    public FuncionarioController(FuncionarioService service, ArquivoStorageService arquivoStorageService) {
        this.service = service;
        this.arquivoStorageService = arquivoStorageService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os funcionários")
    public List<FuncionarioResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um funcionário pelo id")
    public FuncionarioResponse buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/buscar-por-email")
    @Operation(summary = "Busca um funcionário pelo e-mail",
            description = "404 se o e-mail não tem papel de funcionário ainda (mesmo que já exista como pessoa).")
    public FuncionarioResponse buscarPorEmail(@RequestParam String email) {
        return service.buscarPorEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo funcionário (sem senha - ver fluxo de autenticação)")
    public FuncionarioResponse criar(@Valid @RequestBody FuncionarioCreateRequest request) {
        return service.criar(request);
    }

    @PostMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    @Operation(summary = "Sobe (ou substitui) a foto de perfil do funcionário",
            description = "A foto fica na pessoa por trás do funcionário (identidade compartilhada) - só uma por "
                    + "pessoa. Só imagem (jpeg/png/webp/gif) - qualquer outro tipo dá 400.")
    public FuncionarioResponse atualizarFoto(@PathVariable Integer id, @RequestParam MultipartFile arquivo) {
        return service.atualizarFoto(id, arquivo);
    }

    @DeleteMapping("/{id}/foto")
    @Operation(summary = "Remove a foto de perfil do funcionário")
    public FuncionarioResponse removerFoto(@PathVariable Integer id) {
        return service.removerFoto(id);
    }

    @GetMapping("/{id}/foto")
    @Operation(summary = "Serve a imagem da foto de perfil direto (não é mais link assinado do MinIO)",
            description = "Aceita o token JWT via query string (?token=...) além do header Authorization - "
                    + "tag <img> não manda header, ver JwtAuthenticationFilter.")
    public ResponseEntity<InputStreamResource> baixarFoto(@PathVariable Integer id) {
        return arquivoStorageService.baixar(service.chaveFoto(id), null);
    }
}
