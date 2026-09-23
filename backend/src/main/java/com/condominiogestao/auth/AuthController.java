package com.condominiogestao.auth;

import com.condominiogestao.auth.dto.ContextoDto;
import com.condominiogestao.auth.dto.EsqueciSenhaRequest;
import com.condominiogestao.auth.dto.LoginRequest;
import com.condominiogestao.auth.dto.LoginResponse;
import com.condominiogestao.auth.dto.SelecionarContextoRequest;
import com.condominiogestao.auth.dto.TokenResponse;
import com.condominiogestao.auth.dto.TrocarSenhaRequest;
import com.condominiogestao.common.ErrorResponse;
import com.condominiogestao.security.ContextoAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login (CPF/senha), escolha de contexto e senha são públicos (ver {@code @SecurityRequirements}
 * em cada método e o SecurityConfig, que só libera esses 4 caminhos específicos). Os outros dois
 * endpoints daqui (trocar de contexto sem deslogar) exigem token completo, como qualquer outro
 * endpoint do sistema.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login (CPF/senha), escolha/troca de contexto (condomínio + papel) e senha")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login por CPF/senha",
            description = "Se a pessoa tiver só 1 vínculo ativo (condomínio+papel), já devolve o token completo. "
                    + "Se tiver mais de 1, devolve a lista de contextos + um preAuthToken pra usar em /contexto.")
    @ApiResponse(responseCode = "401", description = "CPF/senha inválidos, ou sem nenhum vínculo ativo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/contexto")
    @SecurityRequirements
    @Operation(summary = "Escolhe o contexto (condomínio + papel) e emite o token completo",
            description = "Requer o preAuthToken devolvido por /login (no header Authorization: Bearer <preAuthToken>).")
    @ApiResponse(responseCode = "401", description = "preAuthToken inválido/expirado, ou vínculo não existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TokenResponse selecionarContexto(
            @RequestHeader("Authorization") String authorization, @Valid @RequestBody SelecionarContextoRequest request) {
        return service.selecionarContexto(extrairToken(authorization), request);
    }

    @PostMapping("/esqueci-senha")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Passo 1 de \"Esqueci minha senha\" - confirma CPF + e-mail e manda um código por e-mail",
            description = "204 se existe uma pessoa com esse CPF e esse e-mail juntos (sem dizer qual dos dois "
                    + "está errado, se algum estiver); 404 caso contrário. Gera um código numérico temporário, "
                    + "grava como a senha da pessoa e manda por e-mail - o passo 2 (/trocar-senha) usa esse "
                    + "código como \"senha atual\".")
    @ApiResponse(responseCode = "404", description = "CPF e e-mail não correspondem a nenhuma pessoa cadastrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        service.esqueciSenha(request);
    }

    @PostMapping("/trocar-senha")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Passo 2 de \"Esqueci minha senha\" - troca a senha, exigindo CPF + e-mail + senha atual",
            description = "Desliga a flag que bloqueia login (Pessoa.precisaTrocarSenha) - é isso que libera a "
                    + "pessoa pra entrar de verdade depois. Nova senha não pode ser igual à senha padrão.")
    @ApiResponse(responseCode = "401", description = "Senha atual inválida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "CPF e e-mail não correspondem a nenhuma pessoa cadastrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void trocarSenha(@Valid @RequestBody TrocarSenhaRequest request) {
        service.trocarSenha(request);
    }

    @GetMapping("/meus-contextos")
    @Operation(summary = "Lista os contextos (condomínio + papel) que a pessoa logada pode assumir",
            description = "Requer token completo. Serve pra montar o modal de troca de perfil sem precisar "
                    + "deslogar - inclui o contexto atual.")
    public List<ContextoDto> meusContextos(@AuthenticationPrincipal ContextoAutenticado contexto) {
        return service.listarMeusContextos(contexto);
    }

    @PostMapping("/trocar-contexto")
    @Operation(summary = "Troca pro contexto (condomínio + papel) informado e emite um novo token completo",
            description = "Requer token completo (diferente de POST /contexto, que usa o preAuthToken do primeiro "
                    + "login). A pessoa continua logada - só troca de perfil, sem passar pela tela de login de novo.")
    @ApiResponse(responseCode = "401", description = "Esse vínculo não existe (ou não está mais ativo)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TokenResponse trocarContexto(
            @AuthenticationPrincipal ContextoAutenticado contexto, @Valid @RequestBody SelecionarContextoRequest request) {
        return service.trocarContexto(contexto, request);
    }

    private String extrairToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader;
    }
}
