package com.condominiogestao.security;

import com.condominiogestao.auth.dto.ContextoDto;
import com.condominiogestao.common.UnauthorizedException;
import com.condominiogestao.pessoa.Pessoa;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emite e valida os dois tipos de token do fluxo de login (ver AuthService):
 * <ul>
 *   <li><b>PRE_AUTH</b>: só prova que cpf+senha bateram - de vida curta, só serve pra
 *       chamar {@code POST /api/auth/contexto} e escolher em qual condomínio/papel
 *       entrar. Não dá acesso a nenhum endpoint de domínio.</li>
 *   <li><b>FULL</b>: token de acesso de verdade, já carrega o condomínio/papel
 *       escolhido - é o que o {@link JwtAuthenticationFilter} aceita.</li>
 * </ul>
 */
@Service
public class JwtService {

    private static final String CLAIM_STAGE = "stage";
    private static final String STAGE_PRE_AUTH = "PRE_AUTH";
    private static final String STAGE_FULL = "FULL";

    private final SecretKey key;
    private final long expiracaoCompletoMs;
    private final long expiracaoPreAuthMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiracao-completo-ms}") long expiracaoCompletoMs,
            @Value("${jwt.expiracao-pre-auth-ms}") long expiracaoPreAuthMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoCompletoMs = expiracaoCompletoMs;
        this.expiracaoPreAuthMs = expiracaoPreAuthMs;
    }

    public String gerarTokenPreAuth(Pessoa pessoa) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(pessoa.getId().toString())
                .claim(CLAIM_STAGE, STAGE_PRE_AUTH)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoPreAuthMs)))
                .signWith(key)
                .compact();
    }

    public String gerarTokenCompleto(Pessoa pessoa, ContextoDto contexto) {
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .subject(pessoa.getId().toString())
                .claim(CLAIM_STAGE, STAGE_FULL)
                .claim("nome", pessoa.getNome())
                .claim("tipoPapel", contexto.tipoPapel().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoCompletoMs)));
        // condominioId fica de fora do token quando nulo (papel de administrador, que é
        // global) - em vez de gravar um claim "condominioId": null.
        if (contexto.condominioId() != null) {
            builder.claim("condominioId", contexto.condominioId());
        }
        if (contexto.perfil() != null) {
            builder.claim("perfil", contexto.perfil().name());
        }
        return builder.signWith(key).compact();
    }

    /** Valida assinatura/expiração, sem checar o "stage" do token. */
    public Claims validar(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Token inválido ou expirado");
        }
    }

    public Claims validarCompleto(String token) {
        Claims claims = validar(token);
        if (!STAGE_FULL.equals(claims.get(CLAIM_STAGE, String.class))) {
            throw new UnauthorizedException("Esse token não é um token de acesso completo");
        }
        return claims;
    }

    public Claims validarPreAuth(String token) {
        Claims claims = validar(token);
        if (!STAGE_PRE_AUTH.equals(claims.get(CLAIM_STAGE, String.class))) {
            throw new UnauthorizedException("Esse token não é um token de pré-autenticação");
        }
        return claims;
    }
}
