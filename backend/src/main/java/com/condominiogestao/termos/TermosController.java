package com.condominiogestao.termos;

import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Termo de responsabilidade (pedido do Romulo) - ver {@link TermosResponsabilidade}. */
@RestController
@RequestMapping("/api/termos")
@Tag(name = "Termo de responsabilidade", description = "Aceite do termo exibido no primeiro login (ou quando a versão muda)")
public class TermosController {

    private final TermosService service;

    public TermosController(TermosService service) {
        this.service = service;
    }

    @PostMapping("/aceitar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registra que quem está logado aceitou a versão vigente do termo",
            description = "Grava Pessoa.termosVersaoAceita/termosAceitosEm - some o aviso bloqueante no frontend.")
    public void aceitar(@AuthenticationPrincipal ContextoAutenticado contexto) {
        service.aceitar(contexto);
    }
}
