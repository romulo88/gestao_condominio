package com.condominiogestao.security;

import com.condominiogestao.common.UnauthorizedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê o header {@code Authorization: Bearer <token>}, valida como token FULL e, se for
 * válido, autentica a requisição com um {@link ContextoAutenticado} como principal.
 * Não mexe nos 4 caminhos públicos de {@code /api/auth} (login/seleção de contexto/senha,
 * que lidam com o token pré-auth manualmente) nem no Swagger - os outros dois de
 * {@code /api/auth} (trocar de contexto sem deslogar) PRECISAM passar por aqui, senão
 * nunca ficam autenticados mesmo com token válido (ver SecurityConfig, mesma lista).
 *
 * <p>Também aceita o token via query string (só em GET, só nos poucos caminhos que servem
 * imagem direto - ver {@link #tokenViaQueryStringSePermitido}), porque tag {@code <img
 * src="...">} não manda o header {@code Authorization} - o navegador só sabe pedir esse
 * recurso do jeito que o HTML manda.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/contexto")
                || path.equals("/api/auth/esqueci-senha")
                || path.equals("/api/auth/trocar-senha")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ")
                ? header.substring(7)
                : tokenViaQueryStringSePermitido(request);
        if (token != null) {
            try {
                Claims claims = jwtService.validarCompleto(token);
                Number condominioIdNum = claims.get("condominioId", Number.class);
                ContextoAutenticado contexto = new ContextoAutenticado(
                        Integer.valueOf(claims.getSubject()),
                        claims.get("nome", String.class),
                        condominioIdNum != null ? condominioIdNum.intValue() : null,
                        claims.get("tipoPapel", String.class),
                        claims.get("perfil", String.class));
                var authentication = new UsernamePasswordAuthenticationToken(contexto, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UnauthorizedException ex) {
                // Deixa sem autenticação - o Spring Security barra depois (401) se o
                // endpoint exigir login, com uma mensagem padrão (não a nossa).
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    /** Token pela query string (`?token=...`) só pra GET nos endpoints que servem imagem
     * direto (`.../arquivo`, `.../foto`, `.../gif` - ver `ArquivoStorageService` e os
     * controllers que o usam). Deliberadamente restrito: nenhum outro endpoint aceita
     * token pela URL (evita normalizar token em log de acesso/histórico do navegador pra
     * chamadas que já funcionam bem com o header comum). */
    private String tokenViaQueryStringSePermitido(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getServletPath();
        boolean caminhoDeArquivo = path.endsWith("/arquivo") || path.endsWith("/foto") || path.endsWith("/gif");
        return caminhoDeArquivo ? request.getParameter("token") : null;
    }
}
