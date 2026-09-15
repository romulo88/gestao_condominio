package com.condominiogestao.demanda.dto;

import java.time.LocalDateTime;

/**
 * Um evento de mudança de status numa demanda do morador (alerta de login, pedido do
 * Romulo). {@code tipo} é {@code "aprovada"}, {@code "reprovada"} ou {@code "kanban"}:
 * <ul>
 *   <li>{@code aprovada}/{@code reprovada}: {@code justificativa} preenchida, colunas
 *       ambas nulas.</li>
 *   <li>{@code kanban}: {@code colunaAnteriorNome}/{@code colunaNovaNome} preenchidas,
 *       {@code justificativa} nula - só movimentação de verdade (a entrada inicial na
 *       primeira coluna, que acontece junto com a aprovação, não vira uma linha própria
 *       aqui pra não duplicar o mesmo instante como dois eventos).</li>
 * </ul>
 * {@code origem} é {@code "propria"} (demanda que o morador abriu) ou
 * {@code "acompanhada"} (demanda de outra pessoa que ele marcou "Acompanhar" - v116) -
 * o frontend usa isso pra deixar claro de qual das duas é o evento, já que agora o
 * alerta pode misturar as duas.
 */
public record DemandaMudancaStatusResponse(
        Integer demandaId,
        String demandaTitulo,
        String tipo,
        String colunaAnteriorNome,
        String colunaNovaNome,
        String justificativa,
        LocalDateTime data,
        String origem) {
}
