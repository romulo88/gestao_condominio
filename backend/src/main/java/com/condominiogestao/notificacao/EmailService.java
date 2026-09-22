package com.condominiogestao.notificacao;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mail via SMTP (pedido do Romulo: avisar o morador quando a demanda dele é
 * aprovada/reprovada). Best-effort de propósito: falha de envio (SMTP não configurado,
 * provedor fora do ar, credencial errada) nunca pode impedir a ação de negócio que disparou
 * o e-mail - só loga um aviso e segue (ver {@link #enviar}).
 *
 * <p>Assíncrono (pedido do Romulo): o handshake SMTP inteiro (TCP + TLS + autenticação +
 * envio) pode levar de meio segundo a alguns segundos com provedor compartilhado - sem
 * {@code @Async}, quem clica em aprovar/reprovar/mover ficava esperando essa demora antes
 * de ver a resposta. Só funciona porque {@code enviar} recebe só tipos simples
 * (String/record), nunca entidade JPA - nada de lazy-loading fora da transação original que
 * chamou (ver {@link EmailExecutorConfig} pro pool dedicado). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String host;
    private final String remetenteConfigurado;
    private final String usernamePadrao;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${notificacao.email-remetente:}") String remetenteConfigurado,
            @Value("${spring.mail.username:}") String usernamePadrao) {
        this.mailSender = mailSender;
        this.host = host;
        this.remetenteConfigurado = remetenteConfigurado;
        this.usernamePadrao = usernamePadrao;
    }

    /** Envia HTML com fallback em texto puro (multipart/alternative) - {@code corpo.texto()}
     * é o que aparece pros clientes que não renderizam HTML. Roda em background
     * ({@code emailExecutor}) - o chamador não espera o SMTP responder. */
    @Async("emailExecutor")
    public void enviar(String destinatario, String assunto, EmailTemplates.CorpoEmail corpo) {
        if (host == null || host.isBlank()) {
            log.warn("Envio de e-mail pulado (SMTP não configurado - defina MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD): "
                    + "destinatario={}, assunto={}", destinatario, assunto);
            return;
        }
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            String remetente = remetenteConfigurado.isBlank() ? usernamePadrao : remetenteConfigurado;
            if (!remetente.isBlank()) {
                helper.setFrom(remetente);
            }
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo.texto(), corpo.html());
            mailSender.send(mensagem);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail pra {}: {}", destinatario, e.getMessage());
        }
    }
}
