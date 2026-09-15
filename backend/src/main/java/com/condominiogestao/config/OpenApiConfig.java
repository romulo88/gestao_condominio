package com.condominiogestao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados exibidos no topo do Swagger UI (/swagger-ui.html) e o esquema Bearer, que faz
 * aparecer o botão "Authorize" - cole ali o token de POST /api/auth/login (ou /contexto)
 * pra testar os endpoints protegidos direto na tela.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI condominioGestaoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Condomínio Gestão API")
                        .description("Sistema de gestão de demandas de condomínios - cadastros, "
                                + "aprovação de demandas, quadro Kanban e etiquetas.")
                        .version("v0.1.0")
                        .contact(new Contact().name("Romulo Andrade")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
