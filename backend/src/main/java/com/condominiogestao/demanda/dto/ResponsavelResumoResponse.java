package com.condominiogestao.demanda.dto;

/**
 * Versão enxuta de {@link DemandaResponsavelResponse}, só o suficiente pro card do Kanban
 * desenhar o avatar (foto ou iniciais) de quem está atribuído - sem id da atribuição nem
 * CPF, que só a tela de gerenciar responsáveis (modal de detalhe) precisa.
 */
public record ResponsavelResumoResponse(Integer funcionarioId, String nome, String fotoUrl) {}
