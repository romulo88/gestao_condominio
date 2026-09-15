package com.condominiogestao.condominio.dto;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioTipo;
import java.time.LocalDateTime;

public record CondominioResponse(
        Integer id,
        String nome,
        String cnpj,
        CondominioTipo tipo,
        Situacao situacao,
        Integer quantidadeCasas,
        /** Só preenchido quando {@code tipo = apartamento} - contagem de {@code Bloco}, calculada
         * no service (não é coluna própria). Null quando {@code tipo = casas}. */
        Integer quantidadeBlocos,
        /** Funcionários com vínculo ativo NESSE condomínio e situação global ativa - calculado
         * no service, não é coluna própria (ver FuncionarioCondominioRepository.countAtivosPorCondominio). */
        Integer quantidadeFuncionariosAtivos,
        /** Mesma ideia de quantidadeFuncionariosAtivos, mas pra morador (ver
         * MoradorCondominioRepository.countAtivosPorCondominio). */
        Integer quantidadeMoradoresAtivos,
        /** Link assinado (expira em 15min) - null quando o condomínio não tem GIF
         * cadastrado (opcional). Ver {@code CondominioGifService}. */
        String gifUrl,
        /** Token do link público de leitura do Kanban (pedido do Romulo) - null quando
         * nunca foi gerado (ou já foi revogado). O frontend monta a URL completa
         * (`/kanban-publico/{token}`) - o backend só devolve o token puro, nunca a URL,
         * porque não sabe em que origem o frontend está servido. */
        String kanbanPublicoToken,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CondominioResponse from(
            Condominio condominio,
            Integer quantidadeBlocos,
            Integer quantidadeFuncionariosAtivos,
            Integer quantidadeMoradoresAtivos,
            String gifUrl) {
        return new CondominioResponse(
                condominio.getId(),
                condominio.getNome(),
                condominio.getCnpj(),
                condominio.getTipo(),
                condominio.getSituacao(),
                condominio.getQuantidadeCasas(),
                quantidadeBlocos,
                quantidadeFuncionariosAtivos,
                quantidadeMoradoresAtivos,
                gifUrl,
                condominio.getKanbanPublicoToken(),
                condominio.getCreatedAt(),
                condominio.getUpdatedAt());
    }
}
