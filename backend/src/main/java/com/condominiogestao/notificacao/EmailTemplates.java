package com.condominiogestao.notificacao;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.util.HtmlUtils;

/**
 * Corpo (texto + HTML) dos e-mails transacionais do sistema - centralizado aqui pra não
 * misturar markup com regra de negócio nos services que disparam notificação (ver
 * {@code DemandaService.notificarDecisaoDemanda}).
 */
public final class EmailTemplates {

    /** Pedido do Romulo: link direto pra tela de login nos e-mails de senha (trocar/
     * criar conta/resetar) - clica e já cai lá, sem precisar digitar o endereço. Inclui o
     * `basePath` de produção (`/commander`, ver `next.config.ts`/workflow do frontend). */
    private static final String URL_LOGIN = "https://romtechsolucoes.com.br/commander/login";

    private EmailTemplates() {
    }

    /** {@code texto} é o fallback pros clientes que não renderizam HTML - também melhora
     * a pontuação de spam (e-mail só-HTML é sinal ruim pra alguns provedores). */
    public record CorpoEmail(String texto, String html) {
    }

    /** Pedido do Romulo: aviso ao morador quando a demanda dele é aprovada/reprovada.
     * Verde/vermelho seguem a mesma convenção semântica já usada no resto do sistema (ex:
     * contorno de coluna finalística/etapa vencida no Kanban). Campos livres (título,
     * descrição, raia, resposta do funcionário) vêm escapados (`HtmlUtils.htmlEscape`) no
     * HTML - são texto digitado por morador/funcionário, não confiável pra interpolar
     * direto.
     *
     * <p>{@code raiaNome} e {@code respostaFuncionario} são mutuamente exclusivos na
     * prática (nunca os dois juntos) - refletem os três desfechos possíveis de
     * {@code DemandaService}: aprovada COM coluna (só {@code raiaNome}), aprovada SEM
     * coluna/"de imediato" (só {@code respostaFuncionario}, a justificativa), ou reprovada
     * (só {@code respostaFuncionario}, a justificativa). Qualquer um pode vir {@code null}
     * quando não se aplica - a linha correspondente simplesmente não aparece.
     *
     * <p>{@code condominioNome} sempre aparece (pedido do Romulo: "um morador pode morar
     * em vários" condomínios, então precisa ficar claro de qual demanda/condomínio se trata). */
    public static CorpoEmail demandaDecisao(
            Integer id,
            String titulo,
            String descricao,
            boolean aprovada,
            String condominioNome,
            String raiaNome,
            String respostaFuncionario) {
        String situacao = aprovada ? "Aprovada" : "Reprovada";
        String corDestaque = aprovada ? "#059669" : "#dc2626";
        String corBadgeFundo = aprovada ? "#d1fae5" : "#fee2e2";
        String corBadgeTexto = aprovada ? "#065f46" : "#991b1b";
        String situacaoBadge = "<span style=\"display:inline-block;padding:3px 10px;border-radius:12px;"
                + "background-color:" + corBadgeFundo + ";color:" + corBadgeTexto + ";font-size:12px;"
                + "font-weight:bold;\">" + situacao + "</span>";

        List<String[]> linhas = new ArrayList<>();
        linhas.add(new String[] {"Condomínio", HtmlUtils.htmlEscape(condominioNome)});
        linhas.add(new String[] {"Número", "#" + id});
        linhas.add(new String[] {"Título", HtmlUtils.htmlEscape(titulo)});
        linhas.add(new String[] {"Descrição", HtmlUtils.htmlEscape(descricao)});
        if (raiaNome != null) {
            linhas.add(new String[] {"Coluna no quadro", HtmlUtils.htmlEscape(raiaNome)});
        }
        linhas.add(new String[] {"Situação", situacaoBadge});
        if (respostaFuncionario != null) {
            linhas.add(new String[] {"Resposta do funcionário", HtmlUtils.htmlEscape(respostaFuncionario)});
        }

        StringBuilder tabelaHtml = new StringBuilder();
        for (int i = 0; i < linhas.size(); i++) {
            boolean borda = i < linhas.size() - 1;
            tabelaHtml.append(linha(linhas.get(i)[0], linhas.get(i)[1], borda));
        }

        StringBuilder texto = new StringBuilder();
        texto.append("Este é um e-mail informativo - não é necessário respondê-lo.\n\n");
        texto.append("Sua demanda foi ").append(situacao.toLowerCase()).append(".\n\n");
        texto.append("Condomínio: ").append(condominioNome).append("\n");
        texto.append("Número: #").append(id).append("\n");
        texto.append("Título: ").append(titulo).append("\n");
        texto.append("Descrição: ").append(descricao).append("\n");
        if (raiaNome != null) {
            texto.append("Coluna no quadro: ").append(raiaNome).append("\n");
        }
        texto.append("Situação: ").append(situacao).append("\n");
        if (respostaFuncionario != null) {
            texto.append("Resposta do funcionário: ").append(respostaFuncionario).append("\n");
        }

        String html = "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\"><body style=\"margin:0;padding:0;background-color:#f1f5f9;"
                + "font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#f1f5f9;padding:24px 0;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#ffffff;border-radius:8px;overflow:hidden;\">"
                + logoCommander()
                + "<tr><td style=\"background-color:" + corDestaque + ";padding:16px 24px;\">"
                + "<span style=\"color:#ffffff;font-size:13px;font-weight:bold;letter-spacing:0.5px;"
                + "text-transform:uppercase;\">Demanda " + situacao + "</span></td></tr>"
                + "<tr><td style=\"padding:24px;\">"
                + "<p style=\"margin:0 0 16px;color:#334155;font-size:14px;line-height:1.5;\">"
                + "Sua demanda foi <strong>" + situacao.toLowerCase() + "</strong>.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;\">"
                + tabelaHtml
                + "</table></td></tr>"
                + "<tr><td style=\"padding:16px 24px;background-color:#f8fafc;border-top:1px solid #e2e8f0;\">"
                + "<p style=\"margin:0;color:#94a3b8;font-size:12px;\">Este é um e-mail informativo - não é "
                + "necessário respondê-lo.</p></td></tr>"
                + "</table></td></tr></table></body></html>";

        return new CorpoEmail(texto.toString(), html);
    }

    /** Pedido do Romulo: aviso ADICIONAL ao morador quando a demanda dele entra numa
     * coluna finalística do Kanban ({@code StatusKanban.finalistico}) - além do (nunca no
     * lugar do) e-mail de aprovação/reprovação, avisando que o processo terminou de
     * verdade. Título "Demanda Finalizada" em verde (mesmo tom do e-mail de aprovada). */
    public static CorpoEmail demandaFinalizada(
            Integer id, String titulo, String descricao, String condominioNome, String colunaNome) {
        String corDestaque = "#059669";
        String situacaoBadge = "<span style=\"display:inline-block;padding:3px 10px;border-radius:12px;"
                + "background-color:#d1fae5;color:#065f46;font-size:12px;font-weight:bold;\">Finalizada</span>";

        List<String[]> linhas = new ArrayList<>();
        linhas.add(new String[] {"Condomínio", HtmlUtils.htmlEscape(condominioNome)});
        linhas.add(new String[] {"Número", "#" + id});
        linhas.add(new String[] {"Título", HtmlUtils.htmlEscape(titulo)});
        linhas.add(new String[] {"Descrição", HtmlUtils.htmlEscape(descricao)});
        linhas.add(new String[] {"Coluna no quadro", HtmlUtils.htmlEscape(colunaNome)});
        linhas.add(new String[] {"Situação", situacaoBadge});

        StringBuilder tabelaHtml = new StringBuilder();
        for (int i = 0; i < linhas.size(); i++) {
            tabelaHtml.append(linha(linhas.get(i)[0], linhas.get(i)[1], i < linhas.size() - 1));
        }

        String texto = "Este é um e-mail informativo - não é necessário respondê-lo.\n\n"
                + "Sua demanda foi finalizada.\n\n"
                + "Condomínio: " + condominioNome + "\n"
                + "Número: #" + id + "\n"
                + "Título: " + titulo + "\n"
                + "Descrição: " + descricao + "\n"
                + "Coluna no quadro: " + colunaNome + "\n"
                + "Situação: Finalizada\n";

        String html = "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\"><body style=\"margin:0;padding:0;background-color:#f1f5f9;"
                + "font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#f1f5f9;padding:24px 0;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#ffffff;border-radius:8px;overflow:hidden;\">"
                + logoCommander()
                + "<tr><td style=\"background-color:" + corDestaque + ";padding:16px 24px;\">"
                + "<span style=\"color:#ffffff;font-size:13px;font-weight:bold;letter-spacing:0.5px;"
                + "text-transform:uppercase;\">Demanda Finalizada</span></td></tr>"
                + "<tr><td style=\"padding:24px;\">"
                + "<p style=\"margin:0 0 16px;color:#334155;font-size:14px;line-height:1.5;\">"
                + "Sua demanda foi <strong>finalizada</strong>.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;\">"
                + tabelaHtml
                + "</table></td></tr>"
                + "<tr><td style=\"padding:16px 24px;background-color:#f8fafc;border-top:1px solid #e2e8f0;\">"
                + "<p style=\"margin:0;color:#94a3b8;font-size:12px;\">Este é um e-mail informativo - não é "
                + "necessário respondê-lo.</p></td></tr>"
                + "</table></td></tr></table></body></html>";

        return new CorpoEmail(texto, html);
    }

    /** Pedido do Romulo: e-mail do fluxo "Esqueci minha senha" - mesmo padrão visual dos
     * e-mails de demanda, título "Trocar senha" (aqui em azul, não é nem aprovação nem
     * reprovação). Mostra o e-mail usado na solicitação (confirma que é a conta certa) e
     * o código temporário em destaque, seguido da instrução de uso. {@code codigo} não
     * precisa de {@code htmlEscape} - é sempre numérico, gerado pelo próprio sistema (ver
     * {@code AuthService.esqueciSenha}), nunca texto livre digitado por alguém. */
    public static CorpoEmail trocarSenha(String email, String codigo) {
        String corDestaque = "#2563eb";

        String tabelaHtml = linha("E-mail", HtmlUtils.htmlEscape(email), false);

        String codigoHtml = "<div style=\"margin:20px 0;padding:16px;background-color:#eff6ff;"
                + "border-radius:8px;text-align:center;\">"
                + "<span style=\"font-size:26px;font-weight:bold;letter-spacing:4px;color:#1e3a8a;\">"
                + codigo + "</span></div>";

        String instrucoesHtml = "<p style=\"margin:0;color:#334155;font-size:14px;line-height:1.6;text-align:justify;\">"
                + "Use o código acima como sua <strong>senha atual</strong> na tela de login, em "
                + "\"Esqueceu sua senha?\", informando de novo o e-mail acima - em seguida, escolha sua "
                + "senha nova. Se você não pediu essa troca, sua senha foi alterada mesmo assim - avise "
                + "a administração do seu condomínio o quanto antes.</p>";

        String texto = "Este é um e-mail informativo - não é necessário respondê-lo.\n\n"
                + "Você solicitou a troca de senha da sua conta no Commander.\n\n"
                + "E-mail: " + email + "\n\n"
                + "Seu código temporário: " + codigo + "\n\n"
                + "Use o código acima como sua senha atual na tela de login, em \"Esqueceu sua senha?\", "
                + "informando de novo o e-mail acima - em seguida, escolha sua senha nova. Se você não "
                + "pediu essa troca, sua senha foi alterada mesmo assim - avise a administração do seu "
                + "condomínio o quanto antes.\n\n"
                + "Acesse o sistema em: " + URL_LOGIN + "\n";

        String html = "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\"><body style=\"margin:0;padding:0;background-color:#f1f5f9;"
                + "font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#f1f5f9;padding:24px 0;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#ffffff;border-radius:8px;overflow:hidden;\">"
                + logoCommander()
                + "<tr><td style=\"background-color:" + corDestaque + ";padding:16px 24px;\">"
                + "<span style=\"color:#ffffff;font-size:13px;font-weight:bold;letter-spacing:0.5px;"
                + "text-transform:uppercase;\">Trocar senha</span></td></tr>"
                + "<tr><td style=\"padding:24px;\">"
                + "<p style=\"margin:0 0 16px;color:#334155;font-size:14px;line-height:1.5;\">"
                + "Você solicitou a troca de senha da sua conta no Commander.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;\">"
                + tabelaHtml
                + "</table>"
                + codigoHtml
                + instrucoesHtml
                + botaoLogin()
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 24px;background-color:#f8fafc;border-top:1px solid #e2e8f0;\">"
                + "<p style=\"margin:0;color:#94a3b8;font-size:12px;\">Este é um e-mail informativo - não é "
                + "necessário respondê-lo.</p></td></tr>"
                + "</table></td></tr></table></body></html>";

        return new CorpoEmail(texto, html);
    }

    /** Pedido do Romulo: aviso de cadastro novo (funcionário/morador) - explica que a
     * conta foi criada e o que fazer pra acessar. <b>Não manda nenhum código</b> - ver
     * {@link #senhaResetada}/{@link #semCodigo} pro motivo. */
    public static CorpoEmail contaCriada(String email) {
        return semCodigo("Conta criada", "Sua conta no Commander foi criada.", email);
    }

    /** Pedido do Romulo: aviso do botão "Zerar senha" (síndico/sub-síndico/administrador
     * resetando a senha de alguém que esqueceu). Mesmo motivo de {@link #contaCriada} pra
     * não mandar código - ver {@link #semCodigo}. */
    public static CorpoEmail senhaResetada(String email) {
        return semCodigo("Senha resetada", "Sua senha foi resetada por um administrador do seu condomínio.", email);
    }

    /** Base de {@link #contaCriada}/{@link #senhaResetada} - avisa que a pessoa precisa
     * definir uma senha, mas <b>não manda um código aqui</b> de propósito: cadastro novo e
     * "Zerar senha" ligam {@code precisaTrocarSenha}, e a ÚNICA porta de entrada enquanto
     * essa flag estiver ligada é "Esqueci minha senha" ({@code AuthService.esqueciSenha}) -
     * que gera um código NOVO a cada chamada, invalidando qualquer código anterior. Mandar
     * um código aqui também seria enviar um código morto: a pessoa nunca consegue usá-lo
     * direto (o login já bloqueia antes de checar a senha), então ela SEMPRE vai precisar
     * passar por "Esqueci minha senha" de qualquer jeito - e aí o código de lá que vale, não
     * este. Por isso este e-mail só orienta a pessoa a ir direto pra "Esqueci minha senha". */
    private static CorpoEmail semCodigo(String titulo, String introducao, String email) {
        String corDestaque = "#2563eb";

        String tabelaHtml = linha("E-mail", HtmlUtils.htmlEscape(email), false);

        String instrucao = "Pra acessar, vá na tela de login e clique em \"Esqueceu sua senha?\". Informe o "
                + "e-mail acima - você vai receber um código por e-mail pra definir sua senha.";

        String texto = "Este é um e-mail informativo - não é necessário respondê-lo.\n\n"
                + introducao + "\n\n"
                + "E-mail: " + email + "\n\n"
                + instrucao + "\n\n"
                + "Acesse o sistema em: " + URL_LOGIN + "\n";

        String html = "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\"><body style=\"margin:0;padding:0;background-color:#f1f5f9;"
                + "font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#f1f5f9;padding:24px 0;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"480\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background-color:#ffffff;border-radius:8px;overflow:hidden;\">"
                + logoCommander()
                + "<tr><td style=\"background-color:" + corDestaque + ";padding:16px 24px;\">"
                + "<span style=\"color:#ffffff;font-size:13px;font-weight:bold;letter-spacing:0.5px;"
                + "text-transform:uppercase;\">" + titulo + "</span></td></tr>"
                + "<tr><td style=\"padding:24px;\">"
                + "<p style=\"margin:0 0 16px;color:#334155;font-size:14px;line-height:1.5;\">" + introducao
                + "</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;\">"
                + tabelaHtml
                + "</table>"
                + "<p style=\"margin:20px 0 0;color:#334155;font-size:14px;line-height:1.6;text-align:justify;\">"
                + instrucao
                + "</p>"
                + botaoLogin()
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 24px;background-color:#f8fafc;border-top:1px solid #e2e8f0;\">"
                + "<p style=\"margin:0;color:#94a3b8;font-size:12px;\">Este é um e-mail informativo - não é "
                + "necessário respondê-lo.</p></td></tr>"
                + "</table></td></tr></table></body></html>";

        return new CorpoEmail(texto, html);
    }

    /** "Commander" numa barra escura em degradê azul (mesmo estilo de
     * {@code AuthLayout}/{@code BrandMark} no frontend: slate-950 → slate-900 → blue-900).
     * `background-image` com gradiente pra quem suporta, `background-color` de fallback pra
     * quem não suporta (ex: Outlook desktop) - nunca fica sem cor de fundo. Só texto, sem
     * ícone/imagem/emoji de propósito (decisão do Romulo): imagem hospedada aparece quebrada
     * até o destinatário clicar "Exibir imagens" (bloqueio padrão de e-mail novo em Gmail/
     * Outlook), e SVG inline é removido pelo Gmail por segurança - nenhuma opção de ícone
     * aparece de forma confiável sempre, então ficou só o wordmark. */
    private static String logoCommander() {
        return "<tr><td style=\"background-color:#0f172a;background-image:linear-gradient(135deg,#020617,#1e3a8a);"
                + "padding:18px 24px;text-align:center;\">"
                + "<span style=\"color:#ffffff;font-size:16px;font-weight:bold;letter-spacing:0.3px;\">"
                + "Commander</span></td></tr>";
    }

    /** Botão "Ir para o login" (pedido do Romulo) - mesmo azul de destaque dos e-mails de
     * senha, centralizado. `URL_LOGIN` já é absoluta (inclui domínio e `/commander`), então
     * funciona igual em qualquer cliente de e-mail, sem depender de onde o e-mail foi aberto. */
    private static String botaoLogin() {
        return "<div style=\"margin:20px 0;text-align:center;\">"
                + "<a href=\"" + URL_LOGIN + "\" style=\"display:inline-block;padding:10px 28px;"
                + "background-color:#0f172a;color:#ffffff;font-size:14px;font-weight:bold;"
                + "text-decoration:none;border-radius:8px;\">Ir para o login</a></div>";
    }

    private static String linha(String rotulo, String valor, boolean borda) {
        String estiloBorda = borda ? "border-bottom:1px solid #e2e8f0;" : "";
        return "<tr><td style=\"padding:8px 0;" + estiloBorda + "color:#64748b;font-size:12px;"
                + "text-transform:uppercase;width:110px;vertical-align:top;\">" + rotulo + "</td>"
                + "<td style=\"padding:8px 0;" + estiloBorda + "color:#0f172a;font-size:14px;\">" + valor
                + "</td></tr>";
    }
}
