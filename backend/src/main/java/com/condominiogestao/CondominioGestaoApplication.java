package com.condominiogestao;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CondominioGestaoApplication {

    /** Fuso em que o sistema trabalha - o dos condomínios. Troca por variável de ambiente
     * ({@code APP_TIMEZONE}) se um dia houver condomínio em outro fuso. */
    private static final String FUSO_PADRAO = "America/Sao_Paulo";

    public static void main(String[] args) {
        // O sistema guarda e lê horário como LocalDateTime/LocalDate "de parede" (sem fuso) e o
        // frontend mostra do jeito que vem. Então o relógio do servidor TEM que ser o do
        // condomínio: no container de produção (JRE alpine) o fuso padrão da JVM é UTC, e a ronda
        // iniciada 11:39 aparecia como 14:39 (achado do Romulo). Também evita "hoje" virar o dia
        // seguinte às 21h (LocalDate.now() em UTC) nas regras de evento. Precisa rodar ANTES do
        // Spring: o driver do Postgres lê o fuso da JVM ao abrir a conexão.
        String fuso = System.getenv("APP_TIMEZONE");
        TimeZone.setDefault(TimeZone.getTimeZone(fuso == null || fuso.isBlank() ? FUSO_PADRAO : fuso));
        SpringApplication.run(CondominioGestaoApplication.class, args);
    }
}
