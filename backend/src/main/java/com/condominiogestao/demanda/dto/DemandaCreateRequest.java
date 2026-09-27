package com.condominiogestao.demanda.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Não recebe {@code condominioId} nem o solicitante - vêm do contexto do login (o token
 * já diz em qual condomínio e como qual morador/funcionário a pessoa está agindo).
 *
 * <p>{@code identificarSolicitante}: se o nome de quem abre aparece pro funcionário no
 * "Aberta por" da listagem - o formulário já vem com essa caixa desmarcada por padrão
 * pra morador e marcada pra funcionário (decisão da tela, não daqui).
 *
 * <p>{@code rondaId} (opcional, feature "Controle de Rondas"): preenchido só quando a
 * demanda nasce do botão "Nova demanda" na tela de ronda em andamento - o rondista precisa
 * ser o dono dessa ronda e ela precisa ainda estar `em_andamento` (ver
 * {@code DemandaService.criar}). Fora isso, o fluxo de aprovação da demanda é o mesmo de
 * sempre.
 */
public record DemandaCreateRequest(
        @NotBlank(message = "titulo é obrigatório") String titulo,
        @NotBlank(message = "descricao é obrigatória") String descricao,
        boolean sigilosa,
        boolean identificarSolicitante,
        Integer rondaId) {
}
