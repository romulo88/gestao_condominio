package com.condominiogestao.aviso.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Não recebe {@code funcionarioId} (autor) - vem sempre do contexto do login, quem está
 * criando. {@code condominioId} passou a vir daqui (não mais só do contexto) porque um
 * administrador pode estar gerenciando o quadro de avisos de um condomínio que não é o
 * seu (ele não tem condomínio próprio) - a autorização real continua no service
 * ({@code Autorizacao.ehFuncionarioDoCondominio}), então mandar o id aqui não abre brecha.
 */
public record AvisoCreateRequest(
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        @NotBlank(message = "descricao é obrigatória")
                @Size(max = 250, message = "descricao deve ter no máximo 250 caracteres")
                String descricao,
        @FutureOrPresent(message = "dataExpiracao não pode ser no passado") LocalDateTime dataExpiracao) {
}
