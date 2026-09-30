package com.condominiogestao.termos;

/**
 * Texto do termo de responsabilidade (pedido do Romulo) - constante no CÓDIGO, de propósito
 * (nunca em {@code Parametro}): mudar o texto tem peso jurídico, então exige revisão/deploy,
 * não uma edição solta em tela de administrador sem controle de versão.
 *
 * <p>Pra publicar um texto novo: escreva o texto novo em {@link #TEXTO} e SOME a {@link
 * #VERSAO_ATUAL} em 1. No próximo login, toda {@code Pessoa} com {@code termosVersaoAceita}
 * menor que essa constante (ou nunca aceitou - {@code null}) vê o aviso de novo (ver {@code
 * AuthService#login}/{@code TermosService#aceitar}).
 */
public final class TermosResponsabilidade {

    public static final int VERSAO_ATUAL = 1;

    public static final String TEXTO =
            "Ao usar este sistema, você terá acesso a dados de moradores, funcionários e do "
                    + "condomínio (mensagens, demandas, documentos, fotos, entre outros). Esse acesso é "
                    + "registrado e, em áreas sensíveis como mensagens privadas, identificado com marca "
                    + "d'água em nome de quem visualiza.\n\n"
                    + "Você é responsável por manter sigilo sobre o que acessa e não deve fotografar, "
                    + "copiar ou repassar a terceiros nenhuma informação do sistema sem autorização. Em "
                    + "caso de divulgação indevida, a responsabilidade é exclusivamente de quem a "
                    + "praticou — o condomínio e o sistema ficam isentos de qualquer responsabilidade "
                    + "civil decorrente desse ato.\n\n"
                    + "Seus acessos são registrados para fins de segurança e auditoria, conforme a LGPD.";

    private TermosResponsabilidade() {
    }
}
