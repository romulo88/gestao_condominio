package com.condominiogestao.mensagemrapida.dto;

import com.condominiogestao.mensagemrapida.MensagemRapidaCarater;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code condominioId} explícito (e não do contexto): administrador cadastra mensagem
 * rápida em QUALQUER condomínio (aba "Mensagens rápidas" do cadastro de condomínio), não
 * só no próprio - mesma necessidade de {@code EtiquetaCreateRequest}. Ainda assim nunca
 * confiar cegamente nele: o service sempre valida contra
 * {@link com.condominiogestao.common.Autorizacao#exigirAdministradorOuFuncionarioDoCondominio}. */
public record MensagemRapidaCreateRequest(
        @NotNull(message = "condominioId é obrigatório") Integer condominioId,
        @NotBlank(message = "texto é obrigatório")
                @Size(max = 150, message = "texto deve ter no máximo 150 caracteres")
                String texto,
        @NotNull(message = "carater é obrigatório") MensagemRapidaCarater carater) {
}
