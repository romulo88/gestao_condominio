package com.condominiogestao.demanda;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.demanda.dto.CandidatoAcessoResponse;
import com.condominiogestao.demanda.dto.DemandaAcessoSigilosoConcederRequest;
import com.condominiogestao.demanda.dto.DemandaAcessoSigilosoResponse;
import com.condominiogestao.demanda.dto.VisualizadorPorRegraResponse;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioCondominioRepository;
import com.condominiogestao.funcionario.FuncionarioPerfil;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.morador.Morador;
import com.condominiogestao.morador.MoradorCondominioRepository;
import com.condominiogestao.morador.MoradorRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Item 4.8: quem além do solicitante e de síndico/sub-síndico enxerga uma demanda
 * sigilosa (ver {@link DemandaService#listar}). Só quem já gerencia o sigilo dessa
 * demanda (síndico, sub-síndico, ou o funcionário que a marcou como sigilosa - mesmo
 * critério de {@code DemandaService.podeGerenciarSigilo}) pode indicar mais gente.
 */
@Service
@Transactional(readOnly = true)
public class DemandaAcessoSigilosoService {

    private final DemandaAcessoSigilosoRepository repository;
    private final DemandaRepository demandaRepository;
    private final MoradorRepository moradorRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MoradorCondominioRepository moradorCondominioRepository;
    private final FuncionarioCondominioRepository funcionarioCondominioRepository;

    public DemandaAcessoSigilosoService(
            DemandaAcessoSigilosoRepository repository,
            DemandaRepository demandaRepository,
            MoradorRepository moradorRepository,
            FuncionarioRepository funcionarioRepository,
            MoradorCondominioRepository moradorCondominioRepository,
            FuncionarioCondominioRepository funcionarioCondominioRepository) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.moradorRepository = moradorRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.moradorCondominioRepository = moradorCondominioRepository;
        this.funcionarioCondominioRepository = funcionarioCondominioRepository;
    }

    public List<DemandaAcessoSigilosoResponse> listar(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeGerenciar(contexto, demanda);
        return repository.findByDemandaId(demandaId).stream().map(DemandaAcessoSigilosoResponse::from).toList();
    }

    /** Todo morador com vínculo ATIVO, e todo funcionário com vínculo ATIVO e login (perfil
     * preenchido) nesse condomínio - alimenta a combo de busca do "Gerenciar acesso" (por
     * nome), devolvendo o id de cada um pra conceder acesso. Funcionário SEM perfil não tem
     * como abrir o sistema pra ver a demanda mesmo tendo acesso concedido (pedido do Romulo:
     * "não faz sentido colocar uma pessoa sem perfil na listagem") - morador não tem esse
     * filtro porque já sempre loga como morador, não existe "morador sem perfil". Mesma
     * autorização de {@link #listar}. */
    public List<CandidatoAcessoResponse> listarCandidatos(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeGerenciar(contexto, demanda);
        Integer condominioId = demanda.getCondominio().getId();

        List<CandidatoAcessoResponse> candidatos = new ArrayList<>();
        moradorCondominioRepository.findByCondominioId(condominioId).stream()
                .filter(vinculo -> vinculo.getSituacao() == Situacao.ativo
                        && vinculo.getMorador().getSituacao() == Situacao.ativo)
                .forEach(vinculo -> candidatos.add(new CandidatoAcessoResponse(
                        vinculo.getMorador().getId(),
                        vinculo.getMorador().getNome(),
                        "morador",
                        vinculo.getNumeroUnidade(),
                        vinculo.getBloco() != null ? vinculo.getBloco().getNome() : null,
                        null,
                        null)));

        funcionarioCondominioRepository.findByCondominioId(condominioId).stream()
                .filter(vinculo -> vinculo.getSituacao() == Situacao.ativo
                        && vinculo.getFuncionario().getSituacao() == Situacao.ativo
                        && vinculo.getPerfil() != null)
                .forEach(vinculo -> candidatos.add(new CandidatoAcessoResponse(
                        vinculo.getFuncionario().getId(),
                        vinculo.getFuncionario().getNome(),
                        "funcionario",
                        null,
                        null,
                        vinculo.getPerfil(),
                        vinculo.getFuncao())));

        return candidatos.stream().sorted(Comparator.comparing(CandidatoAcessoResponse::nome)).toList();
    }

    /** Item 4.8 (pedido do Romulo: "exibir os que veem por padrão, mediante regra, como
     * síndicos e subsíndicos. Dessa forma quem tem acesso ao card já sabe quem também está
     * vendo") - síndico/sub-síndico ATIVOS do condomínio, mesma regra de {@code
     * DemandaService.podeGerenciarSigilo}, só que aqui devolvendo os NOMES pra exibir na
     * tela do "Gerenciar acesso" (não é concessão explícita, não tem como revogar - some
     * da lista sozinho se a pessoa deixar de ser síndico/sub-síndico ou for desativada).
     * Não inclui o solicitante nem quem marcou como sigilosa - já aparecem em outro lugar
     * da própria tela da demanda, não precisa duplicar aqui. Mesma autorização de
     * {@link #listar}. */
    public List<VisualizadorPorRegraResponse> listarVisualizacaoPorRegra(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeGerenciar(contexto, demanda);
        Integer condominioId = demanda.getCondominio().getId();

        return funcionarioCondominioRepository.findByCondominioId(condominioId).stream()
                .filter(v -> v.getSituacao() == Situacao.ativo && v.getFuncionario().getSituacao() == Situacao.ativo)
                .filter(v -> v.getPerfil() == FuncionarioPerfil.sindico || v.getPerfil() == FuncionarioPerfil.sub_sindico)
                .map(v -> new VisualizadorPorRegraResponse(v.getFuncionario().getNome(), v.getPerfil()))
                .sorted(Comparator.comparing(VisualizadorPorRegraResponse::nome))
                .toList();
    }

    /**
     * Concede pelo id da Pessoa - se ela tiver vínculo ativo como morador E como
     * funcionário nesse condomínio ao mesmo tempo, ganha acesso nos dois papéis (é a
     * mesma pessoa - {@code Morador}/{@code Funcionario} compartilham a mesma PK de
     * {@code Pessoa}, ver {@code CandidatoAcessoResponse.pessoaId}; não faz sentido
     * perguntar "como qual papel"). Pula silenciosamente quem já tinha acesso; só falha
     * se a pessoa não tiver NENHUM vínculo ativo com o condomínio.
     */
    @Transactional
    public List<DemandaAcessoSigilosoResponse> conceder(
            ContextoAutenticado contexto, Integer demandaId, DemandaAcessoSigilosoConcederRequest request) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeGerenciar(contexto, demanda);

        if (!demanda.isSigilosa()) {
            throw new ConflictException("Essa demanda não é sigilosa - não tem sentido conceder acesso a mais ninguém");
        }

        Integer pessoaId = request.pessoaId();
        Integer condominioId = demanda.getCondominio().getId();
        List<DemandaAcessoSigiloso> concedidos = new ArrayList<>();

        moradorRepository.findById(pessoaId).ifPresent(morador -> {
            boolean vinculoAtivo = moradorCondominioRepository.findByMoradorId(morador.getId()).stream()
                    .anyMatch(v -> v.getCondominio().getId().equals(condominioId) && v.getSituacao() == Situacao.ativo);
            if (vinculoAtivo && !repository.existsByDemandaIdAndMoradorId(demandaId, morador.getId())) {
                concedidos.add(salvarAcesso(demanda, TipoPessoa.morador, morador, null));
            }
        });

        funcionarioRepository.findById(pessoaId).ifPresent(funcionario -> {
            boolean vinculoAtivo = funcionarioCondominioRepository.findByFuncionarioId(funcionario.getId()).stream()
                    .anyMatch(v -> v.getCondominio().getId().equals(condominioId) && v.getSituacao() == Situacao.ativo);
            if (vinculoAtivo && !repository.existsByDemandaIdAndFuncionarioId(demandaId, funcionario.getId())) {
                concedidos.add(salvarAcesso(demanda, TipoPessoa.funcionario, null, funcionario));
            }
        });

        if (concedidos.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nenhuma pessoa com id " + pessoaId
                            + " tem vínculo ativo (morador ou funcionário) com esse condomínio, ou já tinha acesso");
        }

        return concedidos.stream().map(DemandaAcessoSigilosoResponse::from).toList();
    }

    @Transactional
    public void revogar(ContextoAutenticado contexto, Integer demandaId, Integer acessoId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeGerenciar(contexto, demanda);

        DemandaAcessoSigiloso acesso = repository
                .findById(acessoId)
                .orElseThrow(() -> new ResourceNotFoundException("Acesso não encontrado: " + acessoId));
        if (!acesso.getDemanda().getId().equals(demandaId)) {
            throw new ResourceNotFoundException("Acesso não encontrado nessa demanda: " + acessoId);
        }
        repository.delete(acesso);
    }

    private DemandaAcessoSigiloso salvarAcesso(Demanda demanda, TipoPessoa tipoPessoa, Morador morador, Funcionario funcionario) {
        DemandaAcessoSigiloso acesso = new DemandaAcessoSigiloso();
        acesso.setDemanda(demanda);
        acesso.setTipoPessoa(tipoPessoa);
        acesso.setMorador(morador);
        acesso.setFuncionario(funcionario);
        return repository.save(acesso);
    }

    /** Mesmo critério de {@code DemandaService.podeGerenciarSigilo} - duplicado de
     * propósito (são só 5 linhas; não vale criar uma dependência cruzada só por isso). */
    private void exigirPodeGerenciar(ContextoAutenticado contexto, Demanda demanda) {
        boolean ehGestor = Autorizacao.ehGestorDoCondominio(contexto, demanda.getCondominio().getId());
        boolean ehMarcador = TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && demanda.getFuncionarioMarcouSigilo() != null
                && demanda.getFuncionarioMarcouSigilo().getId().equals(contexto.pessoaId());
        if (!ehGestor && !ehMarcador) {
            throw new ForbiddenException(
                    "Só síndico, sub-síndico, ou o funcionário que marcou como sigilosa pode gerenciar quem mais vê essa demanda");
        }
    }

    private Demanda buscarDemanda(Integer id) {
        return demandaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }
}
