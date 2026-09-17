package com.condominiogestao.demanda;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.Cpf;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.demanda.dto.CandidatoResponsavelResponse;
import com.condominiogestao.demanda.dto.DemandaResponsavelAtribuirRequest;
import com.condominiogestao.demanda.dto.DemandaResponsavelResponse;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioCondominio;
import com.condominiogestao.funcionario.FuncionarioCondominioRepository;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atribuição de um ou mais funcionários como responsáveis por uma demanda - pedido do
 * Romulo, mesmo espírito do "Gerenciar acesso" sigiloso (item 4.8): busca por CPF/nome
 * numa combo, atribui, remove. Qualquer funcionário do condomínio pode atribuir (não só
 * síndico/sub-síndico - mais amplo que o sigilo de propósito, já que atribuir
 * responsabilidade é uma ação operacional do dia a dia, não uma questão de privacidade).
 */
@Service
@Transactional(readOnly = true)
public class DemandaResponsavelService {

    private final DemandaResponsavelRepository repository;
    private final DemandaRepository demandaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioCondominioRepository funcionarioCondominioRepository;

    public DemandaResponsavelService(
            DemandaResponsavelRepository repository,
            DemandaRepository demandaRepository,
            FuncionarioRepository funcionarioRepository,
            FuncionarioCondominioRepository funcionarioCondominioRepository) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.funcionarioCondominioRepository = funcionarioCondominioRepository;
    }

    public List<DemandaResponsavelResponse> listar(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirFuncionarioDoCondominio(contexto, demanda);
        Map<Integer, FuncionarioCondominio> vinculoPorFuncionario = vinculoPorFuncionarioNoCondominio(demanda.getCondominio().getId());
        return repository.findByDemandaId(demandaId).stream()
                .map(atribuicao -> DemandaResponsavelResponse.from(
                        atribuicao, vinculoPorFuncionario.get(atribuicao.getFuncionario().getId())))
                .toList();
    }

    /** Vínculo (perfil/função) de cada funcionário do condomínio, numa consulta só - evita
     * N+1 na lista de responsáveis (mesmo espírito de {@code
     * ConversaPrivadaService.vinculoPorMoradorNoCondominio}). */
    private Map<Integer, FuncionarioCondominio> vinculoPorFuncionarioNoCondominio(Integer condominioId) {
        return funcionarioCondominioRepository.findByCondominioId(condominioId).stream()
                .collect(Collectors.toMap(v -> v.getFuncionario().getId(), Function.identity()));
    }

    /** Funcionários ativos do condomínio da demanda - alimenta a combo de busca (nome +
     * CPF), em vez de precisar decorar o CPF de quem vai atribuir. */
    public List<CandidatoResponsavelResponse> listarCandidatos(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirFuncionarioDoCondominio(contexto, demanda);
        return funcionarioCondominioRepository.findByCondominioId(demanda.getCondominio().getId()).stream()
                .filter(v -> v.getSituacao() == Situacao.ativo && v.getFuncionario().getSituacao() == Situacao.ativo)
                .map(v -> new CandidatoResponsavelResponse(
                        v.getFuncionario().getCpf(), v.getFuncionario().getNome(), v.getPerfil(), v.getFuncao()))
                .sorted(Comparator.comparing(CandidatoResponsavelResponse::nome))
                .toList();
    }

    @Transactional
    public DemandaResponsavelResponse atribuir(
            ContextoAutenticado contexto, Integer demandaId, DemandaResponsavelAtribuirRequest request) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirFuncionarioDoCondominio(contexto, demanda);

        String cpf = Cpf.normalizar(request.cpf());
        Funcionario funcionario = funcionarioRepository
                .findByPessoaCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum funcionário com o CPF " + request.cpf()));

        FuncionarioCondominio vinculo = funcionarioCondominioRepository.findByFuncionarioId(funcionario.getId()).stream()
                .filter(v -> v.getCondominio().getId().equals(demanda.getCondominio().getId())
                        && v.getSituacao() == Situacao.ativo)
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Esse funcionário não tem vínculo ativo com o condomínio dessa demanda"));

        if (repository.existsByDemandaIdAndFuncionarioId(demandaId, funcionario.getId())) {
            throw new ConflictException("Esse funcionário já está atribuído a essa demanda");
        }

        DemandaResponsavel atribuicao = new DemandaResponsavel();
        atribuicao.setDemanda(demanda);
        atribuicao.setFuncionario(funcionario);

        return DemandaResponsavelResponse.from(repository.save(atribuicao), vinculo);
    }

    @Transactional
    public void remover(ContextoAutenticado contexto, Integer demandaId, Integer atribuicaoId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirFuncionarioDoCondominio(contexto, demanda);

        DemandaResponsavel atribuicao = repository
                .findById(atribuicaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Atribuição não encontrada: " + atribuicaoId));
        if (!atribuicao.getDemanda().getId().equals(demandaId)) {
            throw new ResourceNotFoundException("Atribuição não encontrada nessa demanda: " + atribuicaoId);
        }
        repository.delete(atribuicao);
    }

    /** Mesmo critério já usado em aprovar/reprovar/mover Kanban - duplicado de propósito
     * (ver justificativa equivalente em {@code DemandaAcessoSigilosoService}). */
    private void exigirFuncionarioDoCondominio(ContextoAutenticado contexto, Demanda demanda) {
        boolean autorizado = TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && demanda.getCondominio().getId().equals(contexto.condominioId());
        if (!autorizado) {
            throw new ForbiddenException("Só funcionário deste condomínio pode atribuir responsável pela demanda");
        }
    }

    private Demanda buscarDemanda(Integer id) {
        return demandaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }
}
