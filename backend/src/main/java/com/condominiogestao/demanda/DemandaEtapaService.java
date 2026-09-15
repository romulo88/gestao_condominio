package com.condominiogestao.demanda;

import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.demanda.dto.DemandaEtapaCreateRequest;
import com.condominiogestao.demanda.dto.DemandaEtapaResponse;
import com.condominiogestao.demanda.dto.DemandaEtapaUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subtarefas INTERNAS da demanda (item 4.5) - só funcionário do condomínio da demanda
 * mexe (cria, edita, marca como concluída). Morador também vê a checklist, mas em modo
 * só-leitura (pedido do Romulo: "colocar as etapas para visualização dos moradores sem
 * possibilidade de marcar como concluída") - mesma visibilidade do quadro Kanban/anexos
 * (sigilo incluso, ver {@link DemandaService#podeVer}), não qualquer morador do
 * condomínio. Diferente do {@code status_kanban}: uma demanda tem várias etapas E um
 * status Kanban ao mesmo tempo (ver comentário em {@link Demanda}).
 */
@Service
@Transactional(readOnly = true)
public class DemandaEtapaService {

    private final DemandaEtapaRepository repository;
    private final DemandaRepository demandaRepository;
    private final DemandaService demandaService;

    public DemandaEtapaService(
            DemandaEtapaRepository repository, DemandaRepository demandaRepository, DemandaService demandaService) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.demandaService = demandaService;
    }

    /** Leitura segue a visibilidade da demanda em si (funcionário do condomínio, ou
     * qualquer morador que possa ver essa demanda - sigilo incluso), não só funcionário
     * como as ações de escrita abaixo (ver {@link #exigirFuncionarioDoCondominio}). */
    public List<DemandaEtapaResponse> listarPorDemanda(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        if (!demandaService.podeVer(contexto, demanda)) {
            throw new ForbiddenException("Você não tem acesso a essa demanda");
        }
        return repository.findByDemandaIdOrderByOrdem(demandaId).stream().map(DemandaEtapaResponse::from).toList();
    }

    @Transactional
    public DemandaEtapaResponse criar(ContextoAutenticado contexto, DemandaEtapaCreateRequest request) {
        Demanda demanda = buscarDemanda(request.demandaId());
        exigirFuncionarioDoCondominio(contexto, demanda);

        DemandaEtapa etapa = new DemandaEtapa();
        etapa.setDemanda(demanda);
        etapa.setNome(request.nome());
        etapa.setPrazo(request.prazo());
        etapa.setOrdem(request.ordem() != null ? request.ordem() : 0);

        return DemandaEtapaResponse.from(repository.save(etapa));
    }

    @Transactional
    public DemandaEtapaResponse atualizar(ContextoAutenticado contexto, Integer id, DemandaEtapaUpdateRequest request) {
        DemandaEtapa etapa = buscarEtapa(id);
        exigirFuncionarioDoCondominio(contexto, etapa.getDemanda());

        etapa.setNome(request.nome());
        etapa.setPrazo(request.prazo());
        if (request.ordem() != null) {
            etapa.setOrdem(request.ordem());
        }

        return DemandaEtapaResponse.from(repository.save(etapa));
    }

    /** Marca/desmarca concluída (like um checkbox) - preenche/limpa `concluidaEm` junto. */
    @Transactional
    public DemandaEtapaResponse alternarConcluida(ContextoAutenticado contexto, Integer id) {
        DemandaEtapa etapa = buscarEtapa(id);
        exigirFuncionarioDoCondominio(contexto, etapa.getDemanda());

        etapa.setConcluida(!etapa.isConcluida());
        etapa.setConcluidaEm(etapa.isConcluida() ? LocalDateTime.now() : null);

        return DemandaEtapaResponse.from(repository.save(etapa));
    }

    private void exigirFuncionarioDoCondominio(ContextoAutenticado contexto, Demanda demanda) {
        boolean autorizado = TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && demanda.getCondominio().getId().equals(contexto.condominioId());
        if (!autorizado) {
            throw new ForbiddenException("Só funcionário deste condomínio pode mexer nas etapas da demanda");
        }
    }

    private Demanda buscarDemanda(Integer id) {
        return demandaRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }

    private DemandaEtapa buscarEtapa(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etapa não encontrada: " + id));
    }
}
