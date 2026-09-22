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

    private static String linha(String rotulo, String valor, boolean borda) {
        String estiloBorda = borda ? "border-bottom:1px solid #e2e8f0;" : "";
        return "<tr><td style=\"padding:8px 0;" + estiloBorda + "color:#64748b;font-size:12px;"
                + "text-transform:uppercase;width:110px;vertical-align:top;\">" + rotulo + "</td>"
                + "<td style=\"padding:8px 0;" + estiloBorda + "color:#0f172a;font-size:14px;\">" + valor
                + "</td></tr>";
    }
}
