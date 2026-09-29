package com.condominiogestao.espacocomum;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.espacocomum.dto.EspacoComumCreateRequest;
import com.condominiogestao.espacocomum.dto.EspacoComumResponse;
import com.condominiogestao.espacocomum.dto.EspacoComumUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Espaços de lazer do condomínio (aba nova em Condomínio, pedido do Romulo) - gerenciável
 * (criar/editar/inativar) por qualquer funcionário do condomínio (qualquer perfil) ou por
 * administrador em QUALQUER condomínio, mesma autorização de {@link
 * com.condominiogestao.etiqueta.EtiquetaService}. {@link #listarPorCondominio} também
 * libera morador (ele escolhe o espaço ao cadastrar um Evento).
 */
@Service
@Transactional(readOnly = true)
public class EspacoComumService {

    private final EspacoComumRepository repository;
    private final CondominioRepository condominioRepository;

    public EspacoComumService(EspacoComumRepository repository, CondominioRepository condominioRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
    }

    /** Funcionário/administrador veem todos (ativos ou não - gerenciam o cadastro);
     * morador (escolhendo o local do Evento) só recebe os ativos. */
    public List<EspacoComumResponse> listarPorCondominio(ContextoAutenticado contexto, Integer condominioId) {
        Autorizacao.exigirAdministradorOuFuncionarioOuMoradorDoCondominio(contexto, condominioId);
        List<EspacoComum> espacos = Autorizacao.ehMoradorDoCondominio(contexto, condominioId)
                ? repository.findByCondominioIdAndSituacao(condominioId, Situacao.ativo)
                : repository.findByCondominioId(condominioId);
        return espacos.stream().map(EspacoComumResponse::from).toList();
    }

    @Transactional
    public EspacoComumResponse criar(ContextoAutenticado contexto, EspacoComumCreateRequest request) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, request.condominioId());

        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        if (repository.existsByCondominioIdAndNome(request.condominioId(), request.nome())) {
            throw new ConflictException("Já existe um espaço \"" + request.nome() + "\" nesse condomínio");
        }

        EspacoComum espaco = new EspacoComum();
        espaco.setCondominio(condominio);
        espaco.setNome(request.nome());

        return EspacoComumResponse.from(repository.save(espaco));
    }

    @Transactional
    public EspacoComumResponse atualizar(ContextoAutenticado contexto, Integer id, EspacoComumUpdateRequest request) {
        EspacoComum espaco = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, espaco.getCondominio().getId());

        if (!request.nome().equals(espaco.getNome())
                && repository.existsByCondominioIdAndNomeAndIdNot(espaco.getCondominio().getId(), request.nome(), id)) {
            throw new ConflictException("Já existe um espaço \"" + request.nome() + "\" nesse condomínio");
        }

        espaco.setNome(request.nome());
        if (request.situacao() != null) {
            espaco.setSituacao(request.situacao());
        }

        return EspacoComumResponse.from(repository.save(espaco));
    }

    private EspacoComum buscarEntidadePorId(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Espaço não encontrado: " + id));
    }
}
