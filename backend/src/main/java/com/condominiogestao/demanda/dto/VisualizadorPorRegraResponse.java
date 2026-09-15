package com.condominiogestao.demanda.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;

/**
 * Alguém que já vê uma demanda sigilosa POR REGRA (item 4.8) - hoje só síndico/sub-síndico
 * ATIVOS do condomínio (pedido do Romulo: "exibir os que veem por padrão, mediante regra,
 * como síndicos e subsíndicos. Dessa forma quem tem acesso ao card já sabe quem também
 * está vendo"). Mostrado como informação, sem botão de revogar - diferente de
 * {@link DemandaAcessoSigilosoResponse}, que é concessão explícita e pode ser desfeita.
 */
public record VisualizadorPorRegraResponse(String nome, FuncionarioPerfil perfil) {}
