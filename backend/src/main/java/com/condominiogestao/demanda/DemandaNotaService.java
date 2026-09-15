package com.condominiogestao.demanda;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.demanda.dto.DemandaNotaCreateRequest;
import com.condominiogestao.demanda.dto.DemandaNotaResponse;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.morador.Morador;
import com.condominiogestao.morador.MoradorRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notas de uma demanda (pedido do Romulo): depois de cadastrada, o morador pergunta sobre
 * o andamento e o funcionário responde com outra nota, se julgar necessário. Ver visibilidade/
 * ver {@link DemandaNota}.
 *
 * <p>Ver/criar segue a mesma visibilidade do quadro Kanban ({@link DemandaService#podeVer}) -
 * qualquer funcionário ou morador do condomínio, respeitando sigilo, mesmo espírito de
 * {@link DemandaDocumentoService}. "Marcar como lida" é exclusivo de funcionário (pedido do
 * Romulo).
 */
@Service
@Transactional(readOnly = true)
public class DemandaNotaService {

    private final DemandaNotaRepository repository;
    private final DemandaRepository demandaRepository;
    private final DemandaService demandaService;
    private final MoradorRepository moradorRepository;
    private final FuncionarioRepository funcionarioRepository;

    public DemandaNotaService(
            DemandaNotaRepository repository,
            DemandaRepository demandaRepository,
            DemandaService demandaService,
            MoradorRepository moradorRepository,
            FuncionarioRepository funcionarioRepository) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.demandaService = demandaService;
        this.moradorRepository = moradorRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    public List<DemandaNotaResponse> listar(ContextoAutenticado contexto, Integer demandaId) {
        Demanda demanda = buscarDemanda(demandaId);
        exigirPodeVer(contexto, demanda);
        return repository.findByDemandaIdOrderByCreatedAtAsc(demandaId).stream()
                .map(nota -> DemandaNotaResponse.from(nota, podeResponder(contexto, demanda, nota)))
                .toList();
    }

    /** Cadastra uma nota nova (pergunta) ou uma resposta (quando {@code notaPaiId} vem
     * preenchido). Só até a demanda ser arquivada (pedido do Romulo). Quando quem responde
     * é funcionário, a nota-pai é marcada como lida na mesma tacada - mesmo efeito de
     * {@link #marcarLida}, "responder já prova que leu". A resposta em si (a nota nova, não
     * a nota-pai) também já nasce lida, com {@code lidaEm} na hora da criação (pedido do
     * Romulo: "essa nota de resposta já é final, não precisa lê-la") - ela é a própria
     * resolução da pergunta que responde, não faz sentido alguém precisar "ler" uma
     * resposta pra ela deixar de contar como pendente (ver {@code DemandaService.algumaPendente}). */
    @Transactional
    public DemandaNotaResponse criar(ContextoAutenticado contexto, DemandaNotaCreateRequest request) {
        Demanda demanda = buscarDemanda(request.demandaId());
        exigirPodeVer(contexto, demanda);
        if (demanda.isArquivada()) {
            throw new ConflictException("Essa demanda está arquivada - não dá mais pra cadastrar nota");
        }

        boolean ehFuncionario = TipoPessoa.funcionario.name().equals(contexto.tipoPapel());
        LocalDateTime agora = LocalDateTime.now();

        DemandaNota notaPai = null;
        if (request.notaPaiId() != null) {
            notaPai = repository
                    .findById(request.notaPaiId())
                    .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada: " + request.notaPaiId()));
            if (!notaPai.getDemanda().getId().equals(demanda.getId())) {
                throw new InvalidRequestException("Essa nota não pertence a esta demanda");
            }
            if (ehFuncionario && !notaPai.isLida()) {
                notaPai.setLida(true);
                notaPai.setLidaEm(agora);
                repository.save(notaPai);
            }
        }

        DemandaNota nota = new DemandaNota();
        nota.setDemanda(demanda);
        nota.setNotaPai(notaPai);
        nota.setTexto(request.texto());
        if (ehFuncionario) {
            nota.setFuncionario(buscarFuncionario(contexto.pessoaId()));
        } else {
            nota.setMorador(buscarMorador(contexto.pessoaId()));
        }
        if (notaPai != null) {
            nota.setLida(true);
            nota.setLidaEm(agora);
        }

        DemandaNota salva = repository.save(nota);
        return DemandaNotaResponse.from(salva, podeResponder(contexto, demanda, salva));
    }

    /** Só funcionário marca (pedido do Romulo) - idempotente: se já estava lida, não mexe
     * em {@code lidaEm} de novo. */
    @Transactional
    public DemandaNotaResponse marcarLida(ContextoAutenticado contexto, Integer id) {
        DemandaNota nota =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada: " + id));
        exigirPodeVer(contexto, nota.getDemanda());
        if (!TipoPessoa.funcionario.name().equals(contexto.tipoPapel())) {
            throw new ForbiddenException("Só funcionário pode marcar uma nota como lida");
        }

        if (!nota.isLida()) {
            nota.setLida(true);
            nota.setLidaEm(LocalDateTime.now());
            nota = repository.save(nota);
        }

        return DemandaNotaResponse.from(nota, podeResponder(contexto, nota.getDemanda(), nota));
    }

    private void exigirPodeVer(ContextoAutenticado contexto, Demanda demanda) {
        if (!demandaService.podeVer(contexto, demanda)) {
            throw new ForbiddenException("Você não tem acesso a essa demanda");
        }
    }

    /** Pedido do Romulo: quem abriu a nota não pode responder a ela mesma. Calculado por
     * viewer (mesmo padrão de {@code DemandaResponse.podeGerenciarSigilo}) - além disso,
     * nenhuma nota aceita resposta depois que a demanda é arquivada (mesma regra de
     * {@link #criar}), e nenhuma nota JÁ LIDA aceita resposta (pedido do Romulo: "uma nota
     * lida ou respondida é uma nota lida" - marcar como lida manualmente já resolve, sem
     * precisar também responder; ver {@code DemandaService.algumaPendente}, mesmo critério). */
    private boolean podeResponder(ContextoAutenticado contexto, Demanda demanda, DemandaNota nota) {
        return !demanda.isArquivada() && !ehAutor(contexto, nota) && !nota.isLida();
    }

    private boolean ehAutor(ContextoAutenticado contexto, DemandaNota nota) {
        if (TipoPessoa.funcionario.name().equals(contexto.tipoPapel())) {
            return nota.getFuncionario() != null && nota.getFuncionario().getId().equals(contexto.pessoaId());
        }
        return nota.getMorador() != null && nota.getMorador().getId().equals(contexto.pessoaId());
    }

    private Demanda buscarDemanda(Integer id) {
        return demandaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }

    private Morador buscarMorador(Integer id) {
        return moradorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id));
    }

    private Funcionario buscarFuncionario(Integer id) {
        return funcionarioRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }
}
