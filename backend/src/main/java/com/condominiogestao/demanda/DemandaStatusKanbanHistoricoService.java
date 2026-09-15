package com.condominiogestao.demanda;

import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.demanda.dto.DemandaStatusKanbanHistoricoResponse;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura do histórico de transição de coluna do Kanban (ver
 * {@link DemandaStatusKanbanHistorico}) - as linhas em si já são gravadas por
 * {@link DemandaService#aprovar} e {@link DemandaService#moverKanban}, aqui só expõe a
 * consulta pro ícone de relógio do card (tooltip "há quantos dias nesta coluna" + modal
 * com o histórico completo ao clicar). Mesma visibilidade de quem pode ver a demanda no
 * quadro Kanban (funcionário do condomínio, ou morador do condomínio respeitando sigilo -
 * ver {@link DemandaService#podeVer}). */
@Service
@Transactional(readOnly = true)
public class DemandaStatusKanbanHistoricoService {

    private final DemandaStatusKanbanHistoricoRepository repository;
    private final DemandaRepository demandaRepository;
    private final DemandaService demandaService;

    public DemandaStatusKanbanHistoricoService(
            DemandaStatusKanbanHistoricoRepository repository,
            DemandaRepository demandaRepository,
            DemandaService demandaService) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.demandaService = demandaService;
    }

    /** Em ordem cronológica (mais antiga primeiro) - a última da lista é a transição
     * mais recente, então é dela que vem "desde quando" a demanda está na coluna atual. */
    public List<DemandaStatusKanbanHistoricoResponse> listarPorDemanda(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = demandaRepository
                .findById(demandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + demandaId));
        if (!demandaService.podeVer(contexto, demanda)) {
            throw new ForbiddenException("Você não tem acesso a essa demanda");
        }
        return repository.findByDemandaIdOrderByCreatedAt(demandaId).stream()
                .map(DemandaStatusKanbanHistoricoResponse::from)
                .toList();
    }
}
