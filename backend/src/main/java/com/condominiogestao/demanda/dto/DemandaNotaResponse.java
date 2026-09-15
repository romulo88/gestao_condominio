package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.DemandaNota;
import java.time.LocalDateTime;

/** {@code notaPaiId} nulo = nota raiz (pergunta); preenchido = resposta - o frontend usa
 * isso pra indentar em relação à nota original (pedido do Romulo). {@code autorTipo} é
 * {@code "morador"} ou {@code "funcionario"}. {@code podeResponder} é calculado por quem
 * está vendo (pedido do Romulo: quem abriu a nota não pode responder a ela mesma) - mesmo
 * padrão de {@code DemandaResponse.podeGerenciarSigilo}, uma permissão por viewer calculada
 * no service, não no cliente. */
public record DemandaNotaResponse(
        Integer id,
        Integer demandaId,
        Integer notaPaiId,
        String autorTipo,
        String autorNome,
        String texto,
        boolean lida,
        LocalDateTime lidaEm,
        boolean podeResponder,
        LocalDateTime createdAt) {

    public static DemandaNotaResponse from(DemandaNota nota, boolean podeResponder) {
        boolean autorMorador = nota.getMorador() != null;
        return new DemandaNotaResponse(
                nota.getId(),
                nota.getDemanda().getId(),
                nota.getNotaPai() != null ? nota.getNotaPai().getId() : null,
                autorMorador ? "morador" : "funcionario",
                autorMorador ? nota.getMorador().getNome() : nota.getFuncionario().getNome(),
                nota.getTexto(),
                nota.isLida(),
                nota.getLidaEm(),
                podeResponder,
                nota.getCreatedAt());
    }
}
