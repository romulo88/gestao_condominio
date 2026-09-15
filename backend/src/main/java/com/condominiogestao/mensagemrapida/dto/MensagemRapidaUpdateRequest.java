package com.condominiogestao.mensagemrapida.dto;

import com.condominiogestao.mensagemrapida.MensagemRapidaCarater;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Só corrige texto/caráter - trocar de condomínio não é "editar", é mensagem nova (ver
 * {@code MensagemRapidaCreateRequest}). */
public record MensagemRapidaUpdateRequest(
        @NotBlank(message = "texto é obrigatório")
                @Size(max = 150, message = "texto deve ter no máximo 150 caracteres")
                String texto,
        @NotNull(message = "carater é obrigatório") MensagemRapidaCarater carater) {
}
