package com.condominiogestao.demanda.dto;

import java.util.List;

/** Envelope de paginação de `/demandas` (pedido do Romulo: "paginar a listagem das
 * demandas em 20 registros") - mesmo espírito de {@code PaginaResponse} (`common/`), mas
 * com dois campos a mais: {@code existeNotaNaoLida}/{@code existeEtapaVencida} avisam se
 * existe ALGUMA demanda (dentre as que o usuário tem direito de ver, sigilo incluído) com
 * nota pendente ou etapa vencida - independente do filtro/busca da página atual e
 * independente de estar ou não na página exibida no momento. É o mesmo aviso ambiente que
 * a tela já mostrava antes de a listagem virar paginada ({@code existeNotaNaoLida}/
 * {@code existeEtapaVencida} em `demandas/page.tsx`), só que agora precisa vir do backend
 * porque o frontend deixou de ter a lista inteira em mãos pra calcular sozinho. */
public record DemandaPaginaResponse(
        List<DemandaResponse> itens,
        int pagina,
        int totalPaginas,
        long totalItens,
        boolean existeNotaNaoLida,
        boolean existeEtapaVencida) {
}
