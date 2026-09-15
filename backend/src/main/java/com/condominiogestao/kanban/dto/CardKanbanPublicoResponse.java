package com.condominiogestao.kanban.dto;

import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import java.util.List;

/** Card do quadro Kanban público (pedido do Romulo: link externo, sem login, "sem
 * possibilidade de detalhar os cards"). De propósito, bem mais enxuto que {@code
 * DemandaResponse} - nada de nome de solicitante, responsável, descrição, notas ou
 * etapas: quem só tem o link não tem como se autenticar como ninguém, então o payload
 * não deve carregar informação que só faz sentido pra quem tem uma sessão de verdade
 * (e não deveria carregar dado pessoal nenhum pra um link 100% público). */
public record CardKanbanPublicoResponse(
        Integer id,
        String titulo,
        /** Só as marcadas {@code visivelMorador} - mesmo critério mais restrito já usado
         * pra morador, aplicado aqui com ainda mais razão (link público, sem login nenhum). */
        List<EtiquetaResponse> etiquetas,
        boolean temAnexos) {
}
