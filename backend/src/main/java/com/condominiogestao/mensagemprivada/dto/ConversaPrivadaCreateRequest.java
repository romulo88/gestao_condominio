package com.condominiogestao.mensagemprivada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** {@code destinatariosId} (id do funcionário - CPF saiu do sistema, v177/LGPD) precisa
 * de pelo menos 1 (pedido do Romulo: "endereçada a qualquer funcionário com login daquele
 * condomínio" - pode ser mais de um, ex: síndico e sub-síndico ao mesmo tempo). {@code
 * texto} é a primeira mensagem do chat - uma conversa privada não nasce vazia. */
public record ConversaPrivadaCreateRequest(
        @NotEmpty(message = "Informe ao menos um destinatário") List<Integer> destinatariosId,
        @NotBlank(message = "texto é obrigatório") String texto) {
}
