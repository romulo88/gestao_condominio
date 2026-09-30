package com.condominiogestao.mensagemprivada.dto;

import com.condominiogestao.mensagemprivada.ConversaPrivadaVisualizacao;
import java.time.LocalDateTime;

/** Auditoria (pedido do Romulo) - nunca expõe o conteúdo das mensagens, só quem abriu a
 * conversa e quando. Ver {@code ConversaPrivadaService#listarVisualizacoes}. */
public record ConversaPrivadaVisualizacaoResponse(String nome, String tipo, LocalDateTime visualizadoEm) {

    public static ConversaPrivadaVisualizacaoResponse from(ConversaPrivadaVisualizacao visualizacao) {
        boolean ehMorador = visualizacao.getMorador() != null;
        return new ConversaPrivadaVisualizacaoResponse(
                ehMorador ? visualizacao.getMorador().getNome() : visualizacao.getFuncionario().getNome(),
                ehMorador ? "morador" : "funcionario",
                visualizacao.getVisualizadoEm());
    }
}
