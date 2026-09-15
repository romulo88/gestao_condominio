package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code notaPaiId} nulo = pergunta nova (raiz); preenchido = resposta a uma nota já
 * existente NA MESMA demanda (o service confere isso). Autor (morador ou funcionário) e
 * condomínio vêm do contexto do login, não daqui. */
public record DemandaNotaCreateRequest(
        @NotNull(message = "demandaId é obrigatório") Integer demandaId,
        Integer notaPaiId,
        @NotBlank(message = "texto é obrigatório")
                @Size(max = 150, message = "texto deve ter no máximo 150 caracteres")
                String texto) {
}
