package com.condominiogestao.kanban;

import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.demanda.Demanda;
import com.condominiogestao.demanda.DemandaDocumentoRepository;
import com.condominiogestao.demanda.DemandaEtiquetaRepository;
import com.condominiogestao.demanda.DemandaRepository;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import com.condominiogestao.kanban.dto.CardKanbanPublicoResponse;
import com.condominiogestao.kanban.dto.ColunaKanbanPublicaResponse;
import com.condominiogestao.kanban.dto.KanbanPublicoResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o quadro Kanban público (pedido do Romulo: "deixar o kanban disponível em um
 * link externo, independente do usuário estar logado... mostrando apenas os cards sem
 * sigilo e as raias visíveis"). Sem {@link com.condominiogestao.security.ContextoAutenticado}
 * nenhum - quem chama aqui não está logado, então a visibilidade não depende de papel
 * nenhum, só do que o token do link já autoriza (ver {@code Condominio.kanbanPublicoToken}):
 *
 * <ul>
 *   <li>Coluna: só {@code visivelExternamente = true} - mesmo critério que já esconde
 *       coluna do morador logado, reaproveitado aqui (o pedido foi "as raias visíveis").
 *   <li>Demanda: nunca sigilosa, e só quando já está numa das colunas acima (demanda
 *       ainda pendente, sem coluna, não aparece - mesma regra do quadro logado).
 *   <li>Campos: só o necessário pra desenhar o card (id/título/etiquetas/anexo) - nada de
 *       nome de solicitante, responsável, descrição, nota ou etapa. Um link público não
 *       tem como restringir quem abre, então o payload em si já não pode carregar dado
 *       pessoal nenhum, independente de qualquer checagem de sigilo.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class KanbanPublicoService {

    private final CondominioRepository condominioRepository;
    private final StatusKanbanRepository statusKanbanRepository;
    private final DemandaRepository demandaRepository;
    private final DemandaEtiquetaRepository demandaEtiquetaRepository;
    private final DemandaDocumentoRepository demandaDocumentoRepository;

    public KanbanPublicoService(
            CondominioRepository condominioRepository,
            StatusKanbanRepository statusKanbanRepository,
            DemandaRepository demandaRepository,
            DemandaEtiquetaRepository demandaEtiquetaRepository,
            DemandaDocumentoRepository demandaDocumentoRepository) {
        this.condominioRepository = condominioRepository;
        this.statusKanbanRepository = statusKanbanRepository;
        this.demandaRepository = demandaRepository;
        this.demandaEtiquetaRepository = demandaEtiquetaRepository;
        this.demandaDocumentoRepository = demandaDocumentoRepository;
    }

    public KanbanPublicoResponse buscarPorToken(String token) {
        Condominio condominio = condominioRepository
                .findByKanbanPublicoToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Link não encontrado, revogado, ou nunca existiu"));

        List<StatusKanban> colunasVisiveis = statusKanbanRepository.findByCondominioIdOrderByOrdem(condominio.getId())
                .stream()
                .filter(StatusKanban::isVisivelExternamente)
                .toList();
        Set<Integer> idsColunasVisiveis = colunasVisiveis.stream().map(StatusKanban::getId).collect(Collectors.toSet());

        List<Demanda> demandas = demandaRepository.findByCondominioId(condominio.getId()).stream()
                .filter(d -> !d.isSigilosa())
                .filter(d -> d.getStatusKanban() != null && idsColunasVisiveis.contains(d.getStatusKanban().getId()))
                // Mesma posição manual do quadro logado (pedido do Romulo) - `ordem` só
                // faz sentido comparado dentro da mesma coluna, mas como cada card só
                // aparece na lista da PRÓPRIA coluna depois do agrupamento abaixo, ordenar
                // a lista inteira por `ordem` já basta pra preservar a ordem certa em cada
                // uma.
                .sorted(Comparator.comparing(Demanda::getOrdem))
                .toList();
        List<Integer> idsDemandas = demandas.stream().map(Demanda::getId).toList();

        Map<Integer, List<EtiquetaResponse>> etiquetasPorDemanda = idsDemandas.isEmpty()
                ? Map.of()
                : demandaEtiquetaRepository.findByDemandaIdInComEtiqueta(idsDemandas).stream()
                        .collect(Collectors.groupingBy(
                                de -> de.getDemanda().getId(),
                                Collectors.mapping(de -> EtiquetaResponse.from(de.getEtiqueta()), Collectors.toList())));
        Set<Integer> idsComAnexo = idsDemandas.isEmpty()
                ? Set.of()
                : demandaDocumentoRepository.findByDemandaIdIn(idsDemandas).stream()
                        .map(documento -> documento.getDemanda().getId())
                        .collect(Collectors.toSet());

        Map<Integer, List<Demanda>> demandasPorColuna =
                demandas.stream().collect(Collectors.groupingBy(d -> d.getStatusKanban().getId()));

        List<ColunaKanbanPublicaResponse> colunas = colunasVisiveis.stream()
                .map(coluna -> new ColunaKanbanPublicaResponse(
                        coluna.getId(),
                        coluna.getNome(),
                        demandasPorColuna.getOrDefault(coluna.getId(), List.of()).stream()
                                .map(d -> new CardKanbanPublicoResponse(
                                        d.getId(),
                                        d.getTitulo(),
                                        etiquetasPorDemanda.getOrDefault(d.getId(), List.of()).stream()
                                                .filter(EtiquetaResponse::visivelMorador)
                                                .toList(),
                                        idsComAnexo.contains(d.getId())))
                                .toList()))
                .toList();

        return new KanbanPublicoResponse(condominio.getNome(), colunas);
    }
}
