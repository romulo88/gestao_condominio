package com.condominiogestao.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envelope padrão de paginação (pedido do Romulo: paginar Funcionários/Moradores do
 * cadastro de condomínio, 15 por página - "a ideia é performance para não listar todos de
 * vez"). Genérico pra reaproveitar em qualquer listagem paginada nova. Não expõe o
 * {@code Page} do Spring direto na API - é um tipo de implementação (`PageImpl`), não
 * pensado pra ser contrato de serialização estável entre versões.
 */
public record PaginaResponse<T>(List<T> itens, int pagina, int totalPaginas, long totalItens) {

    public static <T> PaginaResponse<T> from(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getTotalPages(), pagina.getTotalElements());
    }
}
