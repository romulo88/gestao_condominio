package com.condominiogestao.aviso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Só corrige descrição/expiração - autor e condomínio não mudam por essa tela (trocar de
 * condomínio seria um aviso novo, não uma edição). Diferente de {@code
 * AvisoCreateRequest.dataExpiracao}, aqui não exige {@code @FutureOrPresent} - editar um
 * aviso cuja expiração já passou (por outro motivo qualquer) não deveria travar. */
public record AvisoUpdateRequest(
        @NotBlank(message = "descricao é obrigatória")
                @Size(max = 250, message = "descricao deve ter no máximo 250 caracteres")
                String descricao,
        LocalDateTime dataExpiracao) {
}
