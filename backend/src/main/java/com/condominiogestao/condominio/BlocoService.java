package com.condominiogestao.condominio;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.condominio.dto.BlocoCreateRequest;
import com.condominiogestao.condominio.dto.BlocoResponse;
import com.condominiogestao.condominio.dto.BlocoUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Blocos de condomínio do tipo apartamento (item 1.1) - mesma autorização de
 * {@link com.condominiogestao.etiqueta.EtiquetaService}: funcionário (qualquer perfil) do
 * próprio condomínio, ou administrador em qualquer um. */
@Service
@Transactional(readOnly = true)
public class BlocoService {

    private final BlocoRepository repository;
    private final CondominioRepository condominioRepository;

    public BlocoService(BlocoRepository repository, CondominioRepository condominioRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
    }

    public List<BlocoResponse> listarPorCondominio(ContextoAutenticado contexto, Integer condominioId) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, condominioId);
        return repository.findByCondominioId(condominioId).stream().map(BlocoResponse::from).toList();
    }

    public BlocoResponse buscarPorId(ContextoAutenticado contexto, Integer id) {
        Bloco bloco = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, bloco.getCondominio().getId());
        return BlocoResponse.from(bloco);
    }

    @Transactional
    public BlocoResponse criar(ContextoAutenticado contexto, BlocoCreateRequest request) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, request.condominioId());

        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        if (repository.existsByCondominioIdAndNome(request.condominioId(), request.nome())) {
            throw new ConflictException(
                    "Já existe um bloco \"" + request.nome() + "\" nesse condomínio");
        }

        Bloco bloco = new Bloco();
        bloco.setCondominio(condominio);
        bloco.setNome(request.nome());

        return BlocoResponse.from(repository.save(bloco));
    }

    /** Só corrige o nome - trocar de condomínio é um bloco novo, não uma edição. Mesma
     * autorização de {@link #criar}. */
    @Transactional
    public BlocoResponse atualizar(ContextoAutenticado contexto, Integer id, BlocoUpdateRequest request) {
        Bloco bloco = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, bloco.getCondominio().getId());

        if (!request.nome().equals(bloco.getNome())
                && repository.existsByCondominioIdAndNomeAndIdNot(bloco.getCondominio().getId(), request.nome(), id)) {
            throw new ConflictException("Já existe um bloco \"" + request.nome() + "\" nesse condomínio");
        }

        bloco.setNome(request.nome());

        return BlocoResponse.from(repository.save(bloco));
    }

    private Bloco buscarEntidadePorId(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bloco não encontrado: " + id));
    }
}
