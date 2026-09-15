package com.condominiogestao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe o contexto Spring completo (incluindo Flyway contra o Postgres do
 * docker-compose) e falha se algo não conectar ou as entidades não baterem com o
 * schema (ddl-auto=validate).
 */
@SpringBootTest
class CondominioGestaoApplicationTests {

    @Test
    void contextLoads() {
    }
}
