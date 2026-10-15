package com.condominiogestao.espacocomum.dto;

import com.condominiogestao.common.Situacao;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corrige nome e/ou inativa/reativa - trocar de condomínio não é "editar", é espaço novo. */
public record EspacoComumUpdateRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 50, message = "nome deve ter no máximo 50 caracteres")
                String nome,
        /** Null (campo omitido) não muda a situação atual. */
        Situacao situacao,
        /** DIFERENTE de {@code situacao}: aqui nulo LIMPA o limite ("sem máximo informado") - é
         * o único jeito de apagar o valor. Por isso todo chamador que só quer mexer em outra
         * coisa (ex.: inativar) precisa reenviar o valor atual, senão apaga sem querer. */
        @Min(value = 1, message = "maxVisitantes deve ser pelo menos 1") Integer maxVisitantes) {
}
