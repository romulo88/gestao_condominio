package com.condominiogestao.parametro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code nome} é a chave que o código usa pra ler o valor (ver {@code ParametroService})
 * - cadastrar um parâmetro aqui sem também mudar o código que lê esse nome não tem efeito
 * nenhum (é esperado: a tabela é genérica, quem decide o que É lido é o código). */
public record ParametroCreateRequest(
        @NotBlank(message = "nome é obrigatório")
                @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
                String nome,
        String descricao,
        @NotBlank(message = "valor é obrigatório") String valor) {
}
