package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Reprova a demanda sem passar pelo Kanban - usado quando não vai precisar de nenhuma
 * etapa (ex: já existe outra demanda igual aberta, ou foi resolvida na hora). O motivo
 * fica salvo em {@code justificativa_reprovacao} (e será enviado por e-mail ao morador
 * quando esse fluxo existir - ver observação na entidade {@code Demanda}).
 */
public record DemandaReprovarRequest(@NotBlank(message = "justificativa é obrigatória") String justificativa) {
}
