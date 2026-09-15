package com.condominiogestao.pessoa;

import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.pessoa.dto.PessoaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas", description = "Identidade compartilhada (nome/cpf/email) - consulta apenas")
public class PessoaController {

    private final PessoaService service;

    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @GetMapping("/buscar-por-cpf")
    @Operation(summary = "Busca uma pessoa pelo CPF",
            description = "Usado pra reconhecer alguém já cadastrado (ex: aba Funcionário do cadastro de "
                    + "condomínio) antes de pedir nome/e-mail de novo.")
    @ApiResponse(responseCode = "404", description = "Nenhuma pessoa com esse CPF",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PessoaResponse buscarPorCpf(@RequestParam String cpf) {
        return service.buscarPorCpf(cpf);
    }
}
