package com.condominiogestao.mensagemprivada.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Linha da listagem "minhas conversas" - preview da última mensagem, sem carregar o chat
 * inteiro. {@code pendente} é calculado por quem está vendo (pedido do Romulo: destaque em
 * vermelho quando tem mensagem endereçada a mim, de outro participante, que eu ainda não vi -
 * ver {@code ConversaPrivadaService}), não um flag salvo no banco.
 *
 * <p>{@code autorBlocoNome}/{@code autorNumeroUnidade} só vêm preenchidos quando {@code
 * autorTipo} é {@code "morador"} (pedido do Romulo: identificar de qual unidade é a
 * mensagem, direto na listagem) - {@code autorBlocoNome} fica {@code null} em condomínio
 * de casas (sem bloco) ou se o vínculo não for encontrado. */
public record ConversaPrivadaResumoResponse(
        Integer id,
        String autorTipo,
        String autorNome,
        String autorBlocoNome,
        String autorNumeroUnidade,
        List<ConversaPrivadaDestinatarioResponse> destinatarios,
        String ultimaMensagemTexto,
        String ultimaMensagemAutorNome,
        LocalDateTime ultimaMensagemEm,
        boolean pendente,
        LocalDateTime createdAt) {
}
