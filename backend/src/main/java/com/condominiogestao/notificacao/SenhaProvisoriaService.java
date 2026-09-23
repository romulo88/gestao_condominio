package com.condominiogestao.notificacao;

import com.condominiogestao.pessoa.Pessoa;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Gera um código numérico temporário, grava como a senha da {@link Pessoa} e religa
 * {@code precisaTrocarSenha} - reaproveitado em todo lugar que precisa dar a alguém uma
 * senha provisória de verdade (não mais a senha padrão pública, {@code auth.senha-padrao})
 * pra ela entrar e escolher a própria senha: "Esqueci minha senha"
 * ({@code AuthService.esqueciSenha}), cadastro de novo funcionário/morador
 * ({@code FuncionarioService}/{@code MoradorService.criar}) e "Zerar senha"
 * ({@code FuncionarioCondominioService}/{@code MoradorCondominioService.zerarSenha}).
 *
 * <p><b>Não salva a Pessoa no banco</b> - quem chama decide quando persistir (pode ainda
 * não ter id, ou já estar no meio de outra transação com dirty-checking). Só manda e-mail
 * se a Pessoa tiver e-mail cadastrado - sem e-mail, só grava o código como senha e segue
 * (mesmo espírito de "sem perfil nunca loga" - não faz sentido mandar e-mail nesse caso,
 * mas a senha ainda precisa ser trocável por outro caminho quando um e-mail for cadastrado
 * depois).
 */
@Service
public class SenhaProvisoriaService {

    /** {@code SecureRandom}, não {@code Random}: o código é usado como senha de verdade
     * (ainda que provisória), não pode ser previsível. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public SenhaProvisoriaService(PasswordEncoder passwordEncoder, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void gerarEEnviar(Pessoa pessoa) {
        String codigo = String.format("%08d", RANDOM.nextInt(100_000_000));
        pessoa.setSenhaHash(passwordEncoder.encode(codigo));
        pessoa.setPrecisaTrocarSenha(true);

        if (pessoa.getEmail() != null && !pessoa.getEmail().isBlank()) {
            EmailTemplates.CorpoEmail corpo = EmailTemplates.trocarSenha(pessoa.getCpf(), pessoa.getEmail(), codigo);
            emailService.enviar(pessoa.getEmail(), "Trocar senha", corpo);
        }
    }
}
