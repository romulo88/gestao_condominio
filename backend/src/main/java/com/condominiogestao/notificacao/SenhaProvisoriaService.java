package com.condominiogestao.notificacao;

import com.condominiogestao.pessoa.Pessoa;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Concentra tudo que dá a alguém uma senha provisória (nunca mais a senha padrão pública,
 * {@code auth.senha-padrao}), religando {@code precisaTrocarSenha} - usado por "Esqueci
 * minha senha" ({@code AuthService.esqueciSenha}), cadastro de novo funcionário/morador
 * ({@code FuncionarioService}/{@code MoradorService.criar}) e "Zerar senha"
 * ({@code FuncionarioCondominioService}/{@code MoradorCondominioService.zerarSenha}).
 *
 * <p><b>Só "Esqueci minha senha" manda um código de verdade</b> ({@link #gerarEEnviarCodigo}
 * - usável como "senha atual" no passo 2). Cadastro novo e "Zerar senha" chamam
 * {@link #prepararPrimeiroAcesso}/{@link #resetarEAvisar}, que NÃO mandam código nenhum -
 * pedido do Romulo depois de identificar que o código deles nunca era utilizável: o login
 * bloqueia sempre que {@code precisaTrocarSenha} está ligado (não importa a senha digitada),
 * então a ÚNICA porta de entrada é "Esqueci minha senha" - que gera um código NOVO a cada
 * chamada, invalidando qualquer código anterior. Mandar um código no cadastro/reset seria
 * sempre mandar um código morto (a pessoa NUNCA consegue usá-lo direto), só confundindo com
 * dois e-mails onde só o segundo vale. Por isso esses dois caminhos só avisam por e-mail
 * (sem código) que a pessoa precisa ir em "Esqueci minha senha" pra receber o código real.
 *
 * <p><b>Não salva a Pessoa no banco</b> - quem chama decide quando persistir (pode ainda
 * não ter id, ou já estar no meio de outra transação com dirty-checking). Só manda e-mail
 * se a Pessoa tiver e-mail cadastrado - sem e-mail, só grava a senha descartável e segue
 * (mesmo espírito de "sem perfil nunca loga" - não faz sentido mandar e-mail nesse caso).
 */
@Service
public class SenhaProvisoriaService {

    /** {@code SecureRandom}, não {@code Random}: mesmo os valores descartáveis (cadastro/
     * reset) acabam virando a senha gravada até a pessoa passar por "Esqueci minha senha" -
     * não custa nada garantir que nenhum deles seja previsível. */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public SenhaProvisoriaService(PasswordEncoder passwordEncoder, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** "Esqueci minha senha" (único caminho que de fato precisa dar um código usável). */
    public void gerarEEnviarCodigo(Pessoa pessoa) {
        String codigo = gerarCodigo();
        pessoa.setSenhaHash(passwordEncoder.encode(codigo));
        pessoa.setPrecisaTrocarSenha(true);

        if (temEmail(pessoa)) {
            EmailTemplates.CorpoEmail corpo = EmailTemplates.trocarSenha(pessoa.getEmail(), codigo);
            emailService.enviar(pessoa.getEmail(), "Trocar senha", corpo);
        }
    }

    /** Cadastro de funcionário/morador novo - avisa, não manda código (ver javadoc da classe). */
    public void prepararPrimeiroAcesso(Pessoa pessoa) {
        descartarSenhaAtual(pessoa);
        if (temEmail(pessoa)) {
            EmailTemplates.CorpoEmail corpo = EmailTemplates.contaCriada(pessoa.getEmail());
            emailService.enviar(pessoa.getEmail(), "Conta criada", corpo);
        }
    }

    /** "Zerar senha" (síndico/sub-síndico/administrador) - mesma ideia de
     * {@link #prepararPrimeiroAcesso}, e-mail diferente. */
    public void resetarEAvisar(Pessoa pessoa) {
        descartarSenhaAtual(pessoa);
        if (temEmail(pessoa)) {
            EmailTemplates.CorpoEmail corpo = EmailTemplates.senhaResetada(pessoa.getEmail());
            emailService.enviar(pessoa.getEmail(), "Senha resetada", corpo);
        }
    }

    /** Grava um valor descartável (nunca é usado direto - ver javadoc da classe) só pra
     * {@code senhaHash} não ficar desatualizada/reutilizável, e liga {@code precisaTrocarSenha}. */
    private void descartarSenhaAtual(Pessoa pessoa) {
        pessoa.setSenhaHash(passwordEncoder.encode(gerarCodigo()));
        pessoa.setPrecisaTrocarSenha(true);
    }

    private boolean temEmail(Pessoa pessoa) {
        return pessoa.getEmail() != null && !pessoa.getEmail().isBlank();
    }

    private String gerarCodigo() {
        return String.format("%08d", RANDOM.nextInt(100_000_000));
    }
}
