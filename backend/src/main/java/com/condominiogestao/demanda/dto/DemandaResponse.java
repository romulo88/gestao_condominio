package com.condominiogestao.demanda.dto;

import com.condominiogestao.demanda.Demanda;
import com.condominiogestao.demanda.DemandaStatusAprovacao;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import java.time.LocalDateTime;
import java.util.List;

public record DemandaResponse(
        Integer id,
        Integer condominioId,
        String titulo,
        String descricao,
        /** "morador" ou "funcionario" - quem abriu. */
        String solicitanteTipo,
        /** Null quando {@code identificarSolicitante} é false - "Aberta por" mostra
         * anônimo nesse caso, mesmo pra funcionário. */
        String solicitanteNome,
        boolean identificarSolicitante,
        DemandaStatusAprovacao statusAprovacao,
        /** Preenchidos só depois que a demanda entra no fluxo de triagem/kanban - null recém-criada. */
        Integer statusKanbanId,
        String statusKanbanNome,
        /** Posição do card dentro da coluna atual (`statusKanbanId`) - pedido do Romulo:
         * arrastar card pra qualquer posição, pra agrupar assuntos parecidos lado a lado.
         * O frontend do Kanban ordena a lista inicial por este campo; depois disso, a
         * ordem local do array já reordenado é que vale (ver `kanban/page.tsx`). Sem
         * significado fora do quadro Kanban. */
        Integer ordem,
        String funcionarioResponsavelNome,
        /** Quem decidiu (aprovou/reprovou) e quando - null enquanto pendente. */
        String funcionarioAprovadorNome,
        LocalDateTime dataAprovacao,
        /** Só preenchida quando reprovada. */
        String justificativaReprovacao,
        /** Só preenchida quando aprovada SEM ir pro Kanban (pedido do Romulo) - null
         * quando aprovada com {@code statusKanbanId}, ou enquanto pendente/reprovada. */
        String justificativaAprovacao,
        boolean sigilosa,
        /** Pedido do Romulo: true quando a demanda foi arquivada no card (só possível numa
         * coluna finalística - ver {@code DemandaService.arquivar}). Card fica em estado
         * arquivado e o botão "Arquivar" some. */
        boolean arquivada,
        /** Item 4.8: true só pra síndico/sub-síndico do condomínio, ou pro funcionário que
         * marcou {@code sigilosa = true} mais recentemente - controla se a tela mostra a
         * opção de indicar mais gente (ver `DemandaAcessoSigilosoService`). Sempre false
         * quando a demanda não é sigilosa (não tem o que gerenciar). */
        boolean podeGerenciarSigilo,
        /** Etiquetas já anexadas (item 4.6) - funcionário vê/gerencia todas; morador só
         * recebe a fatia marcada como {@code visivelMorador} (pedido do Romulo), sem poder
         * gerenciar (ver `DemandaEtiquetaService`/`DemandaService.listar`). */
        List<EtiquetaResponse> etiquetas,
        /** Item 4.9: true se a demanda já tem pelo menos uma imagem anexada - controla o
         * destaque (verde/cinza) do ícone de upload no card do Kanban, sem precisar
         * carregar a lista de anexos inteira só pra saber se tem algum. */
        boolean temAnexos,
        /** Pedido do Romulo: true quando a demanda tem pelo menos uma nota ainda não lida
         * OU sem resposta - controla o ícone de alerta no topo do card do Kanban (ver
         * {@code DemandaService.temNotaPendente}), sem precisar carregar as notas inteiras
         * só pra saber se tem alguma pendente. Visível pros dois papéis (diferente de
         * `responsaveis`), já que tanto morador quanto funcionário se beneficiam de saber
         * que a conversa da demanda tem algo em aberto. */
        boolean temNotaPendente,
        /** Pedido do Romulo: true quando a demanda tem pelo menos uma etapa ({@code
         * DemandaEtapa}) com prazo vencido e ainda não concluída - controla o contorno
         * vermelho de destaque no card do Kanban. Desde a v134 (morador passou a VER a
         * lista de etapas, só leitura) vale pros dois papéis - pedido do Romulo (v136):
         * "para morador, colocar o mesmo padrão visual de contornos de etapas no kanban". */
        boolean temEtapaVencida,
        /** Pedido do Romulo: true quando a demanda tem pelo menos uma etapa com prazo
         * marcado, ainda não vencido (hoje ou depois) e ainda não concluída - controla o
         * contorno verde de destaque no card do Kanban. Se a mesma demanda tem etapa
         * vencida E etapa vigente ao mesmo tempo, o vermelho de {@code temEtapaVencida}
         * prevalece (pedido do Romulo) - a UI decide essa prioridade, este campo continua
         * refletindo a realidade (pode vir true junto com {@code temEtapaVencida} true).
         * Mesmo espírito de `temEtapaVencida`: vale pros dois papéis desde a v136. */
        boolean temEtapaVigente,
        /** Funcionários atribuídos - alimenta o avatar (foto ou iniciais) no canto do card
         * do Kanban. Informação interna: vazia pra sessão de morador, mesmo no quadro
         * Kanban - só funcionário vê quem está designado (ver {@code DemandaService.listar}). */
        List<ResponsavelResumoResponse> responsaveis,
        /** Funcionalidade "Acompanhar" (v116): true quando o viewer é morador e NÃO é o
         * solicitante dessa demanda - controla se o card do Kanban mostra o check "Acompanhar"
         * (o morador só pode acompanhar demanda que não é a própria - ver `DemandaService.podeAcompanhar`).
         * Sempre false pra funcionário. */
        boolean podeAcompanhar,
        /** Funcionalidade "Acompanhar" (v116): true quando o MORADOR do contexto atual
         * marcou "Acompanhar" nessa demanda (que ele não abriu) - controla o check no card
         * do Kanban. Sempre false pra funcionário (a funcionalidade é morador-only) e pra
         * demanda que o próprio morador abriu (não tem sentido acompanhar a própria). */
        boolean acompanhando,
        /** Desde quando a demanda está na coluna ATUAL dela (a transição mais recente do
         * histórico) - alimenta o KPI de "dias parado" do Kanban (raia finalística/recorrente
         * não entra nessa contagem, ver {@code StatusKanban.finalistico}/{@code recorrente}).
         * Null enquanto a demanda não tem coluna (statusKanbanId também null), ou nos
         * endpoints que não calculam isso em lote (ver overload sem esse parâmetro). */
        LocalDateTime statusKanbanDesde,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** Demanda recém-criada não tem etiqueta/anexo/nota/responsável nenhum ainda - evita
     * consulta à toa. Também não pode estar sendo acompanhada ainda (acabou de nascer). */
    public static DemandaResponse from(Demanda demanda, boolean podeGerenciarSigilo) {
        return from(demanda, List.of(), podeGerenciarSigilo, false, false, false, false, List.of(), false, false);
    }

    /** Overload sem {@code statusKanbanDesde} - usado pelos endpoints que devolvem uma
     * demanda isolada (criar/aprovar/mover/etc.) e por {@code listarPagina}, onde esse
     * cálculo em lote não foi pedido (KPI é só do Kanban, ver {@link #listar}). */
    public static DemandaResponse from(
            Demanda demanda,
            List<EtiquetaResponse> etiquetas,
            boolean podeGerenciarSigilo,
            boolean temAnexos,
            boolean temNotaPendente,
            boolean temEtapaVencida,
            boolean temEtapaVigente,
            List<ResponsavelResumoResponse> responsaveis,
            boolean podeAcompanhar,
            boolean acompanhando) {
        return from(
                demanda,
                etiquetas,
                podeGerenciarSigilo,
                temAnexos,
                temNotaPendente,
                temEtapaVencida,
                temEtapaVigente,
                responsaveis,
                podeAcompanhar,
                acompanhando,
                null);
    }

    public static DemandaResponse from(
            Demanda demanda,
            List<EtiquetaResponse> etiquetas,
            boolean podeGerenciarSigilo,
            boolean temAnexos,
            boolean temNotaPendente,
            boolean temEtapaVencida,
            boolean temEtapaVigente,
            List<ResponsavelResumoResponse> responsaveis,
            boolean podeAcompanhar,
            boolean acompanhando,
            LocalDateTime statusKanbanDesde) {
        boolean solicitanteMorador = demanda.getMoradorSolicitante() != null;
        String nomeSolicitante = solicitanteMorador
                ? demanda.getMoradorSolicitante().getNome()
                : demanda.getFuncionarioSolicitante().getNome();
        return new DemandaResponse(
                demanda.getId(),
                demanda.getCondominio().getId(),
                demanda.getTitulo(),
                demanda.getDescricao(),
                solicitanteMorador ? "morador" : "funcionario",
                demanda.isIdentificarSolicitante() ? nomeSolicitante : null,
                demanda.isIdentificarSolicitante(),
                demanda.getStatusAprovacao(),
                demanda.getStatusKanban() != null ? demanda.getStatusKanban().getId() : null,
                demanda.getStatusKanban() != null ? demanda.getStatusKanban().getNome() : null,
                demanda.getOrdem(),
                demanda.getFuncionarioResponsavel() != null ? demanda.getFuncionarioResponsavel().getNome() : null,
                demanda.getFuncionarioAprovador() != null ? demanda.getFuncionarioAprovador().getNome() : null,
                demanda.getDataAprovacao(),
                demanda.getJustificativaReprovacao(),
                demanda.getJustificativaAprovacao(),
                demanda.isSigilosa(),
                demanda.isArquivada(),
                demanda.isSigilosa() && podeGerenciarSigilo,
                etiquetas,
                temAnexos,
                temNotaPendente,
                temEtapaVencida,
                temEtapaVigente,
                responsaveis,
                podeAcompanhar,
                acompanhando,
                statusKanbanDesde,
                demanda.getCreatedAt(),
                demanda.getUpdatedAt());
    }
}
