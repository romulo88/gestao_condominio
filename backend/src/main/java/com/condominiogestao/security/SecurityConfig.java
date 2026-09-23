package com.condominiogestao.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/contexto",
                                "/api/auth/esqueci-senha",
                                "/api/auth/trocar-senha",
                                // Pedido do Romulo: link público do quadro Kanban, sem login (ver
                                // KanbanPublicoController) - o token em si já é o controle de acesso.
                                "/api/kanban-publico/**",
                                "/swagger-ui/**",
                                // "/swagger-ui.html" nao casa com "/swagger-ui/**" - e o
                                // path que o springdoc publica (application.yml) e o que
                                // as pessoas digitam. Sem isto, o Swagger devolve 403.
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Libera o front Next.js em dev: localhost, IP da rede local (testar pelo celular na
     * mesma Wi-Fi) e domínios de túnel ngrok (testar pelo celular via internet, PWA - ver
     * HANDOFF.md v120). Usa `allowedOriginPatterns` (não `allowedOrigins`) porque precisa
     * de wildcard + `allowCredentials(true)` juntos, o que o Spring só permite assim.
     *
     * Domínio de produção (romtechsolucoes.com.br): mesmo o navegador só falando com uma
     * origem só (o proxy /api/* do Next é servidor-a-servidor, ver DEPLOY.md), o Next
     * repassa o header Origin do navegador sem alterar quando encaminha pro backend - sem
     * essas duas entradas aqui, o CorsFilter do Spring rejeita com 403 "Invalid CORS
     * request" antes mesmo de chegar no AuthService (sintoma: login funciona via curl sem
     * header Origin, mas falha no navegador de verdade).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://192.168.*.*:*",
                "https://*.ngrok-free.dev",
                "https://*.ngrok-free.app",
                "https://*.ngrok.app",
                "https://romtechsolucoes.com.br",
                "https://www.romtechsolucoes.com.br"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
