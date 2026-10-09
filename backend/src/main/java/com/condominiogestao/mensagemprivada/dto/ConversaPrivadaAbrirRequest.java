package com.condominiogestao.mensagemprivada.dto;

import jakarta.validation.constraints.NotBlank;

/** Senha do próprio usuário logado, exigida a cada vez que ele abre uma conversa privada
 * (pedido do Romulo: computador compartilhado - quem cair numa sessão esquecida aberta não
 * lê a conversa sem saber a senha). Ver {@code ConversaPrivadaService#abrir}. */
public record ConversaPrivadaAbrirRequest(@NotBlank(message = "senha é obrigatória") String senha) {
}
