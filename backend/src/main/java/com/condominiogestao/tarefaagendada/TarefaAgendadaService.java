package com.condominiogestao.tarefaagendada;

import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.tarefaagendada.dto.TarefaAgendadaCreateRequest;
import com.condominiogestao.tarefaagendada.dto.TarefaAgendadaResponse;
import com.condominiogestao.tarefaagendada.dto.TarefaAgendadaUpdateRequest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tarefas agendadas do condomínio do PRÓPRIO contexto - quem cria e quem lista é sempre um
 * funcionário logado num condomínio (morador não usa; administrador não tem condomínio
 * próprio). Não recebe {@code condominioId} de fora, diferente do quadro de avisos: aqui
 * não existe visão de administrador supervisionando outro condomínio.
 */
@Service
@Transactional(readOnly = true)
public class TarefaAgendadaService {

    private final TarefaAgendadaRepository repository;
    private final CondominioRepository condominioRepository;
    private final FuncionarioRepository funcionarioRepository;

    public TarefaAgendadaService(
            TarefaAgendadaRepository repository,
            CondominioRepository condominioRepository,
            FuncionarioRepository funcionarioRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    /** Só as tarefas ATIVAS do condomínio do contexto, ordenadas pela data mais próxima
     * entre as três (crescente) - o que vence primeiro (ou já venceu) fica no topo, pra
     * bater com o "sininho" vermelho do menu. */
    public List<TarefaAgendadaResponse> listar(ContextoAutenticado contexto) {
        exigirFuncionario(contexto);
        return repository.findByCondominioIdAndSituacao(contexto.condominioId(), Situacao.ativo).stream()
                .sorted(Comparator.comparing(TarefaAgendada::proximaDataRelevante)
                        .thenComparing(TarefaAgendada::getId))
                .map(TarefaAgendadaResponse::from)
                .toList();
    }

    /** Qualquer funcionário do condomínio pode cadastrar (pedido do Romulo). */
    @Transactional
    public TarefaAgendadaResponse criar(ContextoAutenticado contexto, TarefaAgendadaCreateRequest request) {
        exigirFuncionario(contexto);
        validarDatas(request.dataTarefa(), request.dataPrimeiroAviso(), request.dataSegundoAviso());

        Condominio condominio = condominioRepository
                .findById(contexto.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + contexto.condominioId()));
        Funcionario funcionario = funcionarioRepository
                .findById(contexto.pessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + contexto.pessoaId()));

        TarefaAgendada tarefa = new TarefaAgendada();
        tarefa.setCondominio(condominio);
        tarefa.setFuncionario(funcionario);
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setDataTarefa(request.dataTarefa());
        tarefa.setDataPrimeiroAviso(request.dataPrimeiroAviso());
        tarefa.setDataSegundoAviso(request.dataSegundoAviso());

        return TarefaAgendadaResponse.from(repository.save(tarefa));
    }

    /** Corrige título/descrição/datas - qualquer funcionário do condomínio pode, não só
     * quem cadastrou (mesmo critério de {@link #criar}, não é preciso ser o autor). */
    @Transactional
    public TarefaAgendadaResponse atualizar(ContextoAutenticado contexto, Integer id, TarefaAgendadaUpdateRequest request) {
        TarefaAgendada tarefa = buscarEntidadeDoContexto(contexto, id);
        validarDatas(request.dataTarefa(), request.dataPrimeiroAviso(), request.dataSegundoAviso());

        tarefa.setTitulo(request.titulo());
        tarefa.setDescricao(request.descricao());
        tarefa.setDataTarefa(request.dataTarefa());
        tarefa.setDataPrimeiroAviso(request.dataPrimeiroAviso());
        tarefa.setDataSegundoAviso(request.dataSegundoAviso());

        return TarefaAgendadaResponse.from(repository.save(tarefa));
    }

    /** "Remover" na aplicação (pedido do Romulo) - soft-delete, mesmo espírito de
     * {@code Aviso.desativar}/{@code EtiquetaService}, sem apagar nada do banco. */
    @Transactional
    public TarefaAgendadaResponse excluir(ContextoAutenticado contexto, Integer id) {
        TarefaAgendada tarefa = buscarEntidadeDoContexto(contexto, id);

        tarefa.setSituacao(Situacao.inativo);

        return TarefaAgendadaResponse.from(repository.save(tarefa));
    }

    /** Busca a tarefa e confere que ela é do condomínio do PRÓPRIO contexto - aqui não
     * existe visão de administrador supervisionando outro condomínio (ver javadoc da
     * classe), então "não encontrado" cobre tanto id inexistente quanto tarefa de outro
     * condomínio. */
    private TarefaAgendada buscarEntidadeDoContexto(ContextoAutenticado contexto, Integer id) {
        exigirFuncionario(contexto);
        TarefaAgendada tarefa = repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa agendada não encontrada: " + id));
        if (!tarefa.getCondominio().getId().equals(contexto.condominioId())) {
            throw new ResourceNotFoundException("Tarefa agendada não encontrada: " + id);
        }
        return tarefa;
    }

    /** Regras de validação do formulário (pedido do Romulo): os dois avisos têm que ser
     * ANTES da data da tarefa, os dois avisos não podem cair no mesmo dia, e o segundo
     * aviso não pode ser antes do primeiro - junto, isso força
     * {@code dataPrimeiroAviso < dataSegundoAviso < dataTarefa}. Não dá pra expressar isso
     * só com bean validation (depende de mais de um campo), daí a checagem aqui. */
    private void validarDatas(LocalDate dataTarefa, LocalDate dataPrimeiroAviso, LocalDate dataSegundoAviso) {
        if (!dataPrimeiroAviso.isBefore(dataTarefa)) {
            throw new InvalidRequestException("A data do primeiro aviso não pode ser igual ou depois da data da tarefa");
        }
        if (!dataSegundoAviso.isBefore(dataTarefa)) {
            throw new InvalidRequestException("A data do segundo aviso não pode ser igual ou depois da data da tarefa");
        }
        if (dataPrimeiroAviso.equals(dataSegundoAviso)) {
            throw new InvalidRequestException("As datas dos avisos não podem ser iguais");
        }
        if (dataSegundoAviso.isBefore(dataPrimeiroAviso)) {
            throw new InvalidRequestException("A data do segundo aviso não pode ser antes da data do primeiro aviso");
        }
    }

    private void exigirFuncionario(ContextoAutenticado contexto) {
        if (!TipoPessoa.funcionario.name().equals(contexto.tipoPapel())) {
            throw new ForbiddenException("Só funcionário pode cadastrar/ver tarefas agendadas");
        }
    }
}
