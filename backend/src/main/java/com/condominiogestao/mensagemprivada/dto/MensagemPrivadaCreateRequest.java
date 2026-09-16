package com.condominiogestao.mensagemprivada.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MensagemPrivadaCreateRequest(
        @NotNull(message = "conversaId é obrigatório") Integer conversaId,
        @NotBlank(message = "texto é obrigatório") String texto) {
}
