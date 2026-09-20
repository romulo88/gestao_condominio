package com.condominiogestao.kanban;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.demanda.DemandaRepository;
import com.condominiogestao.demanda.DemandaStatusKanbanHistoricoRepository;
import com.condominiogestao.kanban.dto.StatusKanbanCreateRequest;
import com.condominiogestao.kanban.dto.StatusKanbanResponse;
import com.condominiogestao.kanban.dto.StatusKanbanUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gerenciar as colunas do Kanban é uma decisão de configuração do condomínio, não uma
 * ação do dia a dia (diferente do quadro de avisos, que qualquer funcionário mexe) -
 * por isso exige administrador ou síndico/sub-síndico, igual editar os dados do
 * condomínio (ver {@link Autorizacao#exigirAdministradorOuGestor}).
 */
@Service
@Transactional(readOnly = true)
public class StatusKanbanService {

    private final StatusKanbanRepository repository;
    private final CondominioRepository condominioRepository;
    private final DemandaRepository demandaRepository;
    private final DemandaStatusKanbanHistoricoRepository historicoRepository;

    public StatusKanbanService(
            StatusKanbanRepository repository,
            CondominioRepository condominioRepository,
            DemandaRepository demandaRepository,
            DemandaStatusKanbanHistoricoRepository historicoRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
        this.demandaRepository = demandaRepository;
        this.historicoRepository = historicoRepository;
    }

    /** Morador só vê coluna com {@code visivelExternamente = true} (pedido do Romulo) -
     * funcionário/administrador sempre veem todas, inclusive pra poder reverter uma
     * coluna oculta. Mesma checagem é repetida em {@code DemandaService.listar} pras
     * demandas dentro da coluna - não basta esconder só a coluna, defesa em profundidade. */
    public List<StatusKanbanResponse> listarPorCondominio(ContextoAutenticado contexto, Integer condominioId) {
        boolean ehMorador = TipoPessoa.morador.name().equals(contexto.tipoPapel());
        return repository.findByCondominioIdOrderByOrdem(condominioId).stream()
                .filter(s -> !ehMorador || s.isVisivelExternamente())
                .map(StatusKanbanResponse::from)
                .toList();
    }

    public StatusKanbanResponse buscarPorId(Integer id) {
        return StatusKanbanResponse.from(buscarEntidadePorId(id));
    }

    @Transactional
    public StatusKanbanResponse criar(ContextoAutenticado contexto, StatusKanbanCreateRequest request) {
        Autorizacao.exigirAdministradorOuGestor(contexto, request.condominioId());

        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        if (repository.existsByCondominioIdAndNome(request.condominioId(), request.nome())) {
            throw new ConflictException("Já existe uma coluna \"" + request.nome() + "\" nesse condomínio");
        }

        StatusKanban statusKanban = new StatusKanban();
        statusKanban.setCondominio(condominio);
        statusKanban.setNome(request.nome());
        statusKanban.setOrdem(request.ordem() != null ? request.ordem() : 0);
        statusKanban.setVisivelExternamente(request.visivelExternamente() == null || request.visivelExternamente());
        statusKanban.setFinalistico(request.finalistico() != null && request.finalistico());
        statusKanban.setRecorrente(request.recorrente() != null && request.recorrente());

        return StatusKanbanResponse.from(repository.save(statusKanban));
    }

    @Transactional
    public StatusKanbanResponse atualizar(ContextoAutenticado contexto, Integer id, StatusKanbanUpdateRequest request) {
        StatusKanban statusKanban = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, statusKanban.getCondominio().getId());

        if (!statusKanban.getNome().equals(request.nome())
                && repository.existsByCondominioIdAndNome(statusKanban.getCondominio().getId(), request.nome())) {
            throw new ConflictException("Já existe uma coluna \"" + request.nome() + "\" nesse condomínio");
        }

        statusKanban.setNome(request.nome());
        if (request.ordem() != null) {
            statusKanban.setOrdem(request.ordem());
        }
        if (request.visivelExternamente() != null) {
            statusKanban.setVisivelExternamente(request.visivelExternamente());
        }
        if (request.finalistico() != null) {
            statusKanban.setFinalistico(request.finalistico());
        }
        if (request.recorrente() != null) {
            statusKanban.setRecorrente(request.recorrente());
        }

        return StatusKanbanResponse.from(repository.save(statusKanban));
    }

    /**
     * Exclui a coluna de verdade (não é soft-delete - diferente de Etiqueta/MensagemRapida,
     * uma coluna sem card nenhum não tem motivo pra continuar ocupando espaço na tela).
     * Pedido do Romulo: só permite excluir enquanto não tiver NENHUMA demanda nela agora -
     * senão, mensagem pedindo pra tirar os cards antes.
     */
    @Transactional
    public void excluir(ContextoAutenticado contexto, Integer id) {
        StatusKanban statusKanban = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, statusKanban.getCondominio().getId());

        if (demandaRepository.existsByStatusKanbanId(id)) {
            throw new ConflictException("Retire os cards antes de excluir");
        }

        // Uma coluna sem card AGORA ainda pode ter linha de histórico de um card que já
        // passou por ali antes de se mudar pra outra - precisa limpar antes, senão a
        // constraint de chave estrangeira do banco barra a exclusão da coluna (ver nota em
        // DemandaStatusKanbanHistoricoRepository.deleteByStatusAnteriorIdOrStatusNovoId).
        historicoRepository.deleteByStatusAnteriorIdOrStatusNovoId(id, id);
        repository.delete(statusKanban);
    }

    private StatusKanban buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coluna de Kanban não encontrada: " + id));
    }
}
