package com.condominiogestao.notificacao;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool dedicado pro envio de e-mail em segundo plano (ver {@link EmailService#enviar}) -
 * pedido do Romulo: aprovar/reprovar/mover demanda não pode esperar o handshake SMTP
 * inteiro (TCP + TLS + autenticação + envio - às vezes alguns segundos com provedor
 * compartilhado como a Hostinger). Pool pequeno de propósito - é só notificação, não
 * disputa recursos com o resto da aplicação; fila generosa porque enfileirar um pouco mais
 * é sempre melhor que perder o e-mail.
 */
@Configuration
@EnableAsync
public class EmailExecutorConfig {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }
}
