package com.condominiogestao.espacocomum.dto;

import com.condominiogestao.common.Situacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corrige nome e/ou inativa/reativa - trocar de condomínio não é "editar", é espaço novo. */
public record EspacoComumUpdateRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 50, message = "nome deve ter no máximo 50 caracteres")
                String nome,
        /** Null (campo omitido) não muda a situação atual. */
        Situacao situacao) {
}
