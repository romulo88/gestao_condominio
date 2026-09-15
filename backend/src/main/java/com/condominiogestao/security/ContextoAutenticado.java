package com.condominiogestao.security;

/**
 * "Quem está logado e como" - o condomínio/papel escolhidos no login (ver AuthService),
 * extraídos do token completo pelo {@link JwtAuthenticationFilter}. Vira o principal do
 * {@code Authentication} do Spring Security; controllers pegam com
 * {@code @AuthenticationPrincipal ContextoAutenticado contexto}.
 */
public record ContextoAutenticado(
        Integer pessoaId, String nome, Integer condominioId, String tipoPapel, String perfil) {
}
