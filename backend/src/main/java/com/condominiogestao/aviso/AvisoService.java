package com.condominiogestao.aviso;

import com.condominiogestao.aviso.dto.AvisoCreateRequest;
import com.condominiogestao.aviso.dto.AvisoResponse;
import com.condominiogestao.aviso.dto.AvisoUpdateRequest;
import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Mural" (listarVisiveis) sempre opera no condomínio do próprio contexto (só faz
 * sentido pra quem está logado como funcionário/morador de um condomínio). Os métodos de
 * gestão (listarTodos/criar/desativar) recebem o condomínio explicitamente, porque um
 * administrador pode estar gerenciando o quadro de avisos de um condomínio que não é o
 * dele (ele não tem nenhum) - a autorização de verdade está em {@link Autorizacao}, não
 * em confiar cegamente no id recebido.
 */
@Service
@Transactional(readOnly = true)
public class AvisoService {

    private final AvisoRepository repository;
    private final CondominioRepository condominioRepository;
    private final FuncionarioRepository funcionarioRepository;

    public AvisoService(
            AvisoRepository repository,
            CondominioRepository condominioRepository,
            FuncionarioRepository funcionarioRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    /** O "mural": só os avisos visíveis agora, do condomínio do PRÓPRIO contexto - pra
     * funcionário ou morador ao logar. Administrador não tem condomínio próprio, então
     * não usa esse endpoint (usa listarTodos com o id explícito, na tela de gestão). */
    public List<AvisoResponse> listarVisiveis(ContextoAutenticado contexto) {
        return repository.findVisiveisPorCondominio(contexto.condominioId(), LocalDateTime.now()).stream()
                .map(AvisoResponse::from)
                .toList();
    }

    /** Visão de gestão: todos os avisos de UM condomínio (qualquer situação/expiração) -
     * administrador (qualquer condomínio) ou funcionário desse condomínio (qualquer perfil). */
    public List<AvisoResponse> listarTodos(ContextoAutenticado contexto, Integer condominioId) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, condominioId);
        return repository.findByCondominioIdOrderByFixadoNoTopoDescCreatedAtDesc(condominioId).stream()
                .map(AvisoResponse::from)
                .toList();
    }

    public AvisoResponse buscarPorId(ContextoAutenticado contexto, Integer id) {
        return AvisoResponse.from(buscarEntidadeVisivelParaContexto(contexto, id));
    }

    /** Só funcionário do condomínio pode REDIGIR um aviso (precisa ser o autor) -
     * diferente de listar/desativar, administrador não pode criar em nome de ninguém
     * porque ele não é funcionário de lugar nenhum (a não ser que também tenha esse
     * papel e esteja logado com ele - aí é só trocar de contexto, ver /api/auth/contexto). */
    @Transactional
    public AvisoResponse criar(ContextoAutenticado contexto, AvisoCreateRequest request) {
        if (!Autorizacao.ehFuncionarioDoCondominio(contexto, request.condominioId())) {
            throw new ForbiddenException("Só funcionário deste condomínio pode criar aviso");
        }

        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));
        Funcionario funcionario = funcionarioRepository
                .findById(contexto.pessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + contexto.pessoaId()));

        Aviso aviso = new Aviso();
        aviso.setCondominio(condominio);
        aviso.setFuncionario(funcionario);
        aviso.setDescricao(request.descricao());
        aviso.setDataExpiracao(request.dataExpiracao());

        return AvisoResponse.from(repository.save(aviso));
    }

    /** Corrige descrição/expiração de um aviso já existente - mesma autorização de {@link
     * #desativar} (não exige ser o autor original, qualquer funcionário do condomínio ou
     * administrador pode corrigir). Autor e condomínio não mudam por essa tela. */
    @Transactional
    public AvisoResponse atualizar(ContextoAutenticado contexto, Integer id, AvisoUpdateRequest request) {
        Aviso aviso =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aviso não encontrado: " + id));
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, aviso.getCondominio().getId());

        aviso.setDescricao(request.descricao());
        aviso.setDataExpiracao(request.dataExpiracao());

        return AvisoResponse.from(repository.save(aviso));
    }

    /** Administrador (qualquer condomínio, supervisão) ou funcionário desse condomínio
     * (qualquer perfil) - desativar não exige ser o autor original do aviso. */
    @Transactional
    public AvisoResponse desativar(ContextoAutenticado contexto, Integer id) {
        Aviso aviso =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aviso não encontrado: " + id));
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, aviso.getCondominio().getId());

        aviso.setSituacao(Situacao.inativo);

        return AvisoResponse.from(repository.save(aviso));
    }

    /** Fixa este aviso no topo do quadro - reservado pra "informações úteis" (ex: telefones
     * da administração), pedido do Romulo. Desfixa automaticamente qualquer outro aviso do
     * MESMO condomínio que já estivesse fixado, na mesma transação (só 1 por vez - ver
     * também o índice único parcial da V24). Mesma autorização de {@link #desativar}. */
    @Transactional
    public AvisoResponse fixarNoTopo(ContextoAutenticado contexto, Integer id) {
        Aviso aviso =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aviso não encontrado: " + id));
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, aviso.getCondominio().getId());

        repository.findByCondominioIdAndFixadoNoTopoTrue(aviso.getCondominio().getId())
                .filter(fixadoAtual -> !fixadoAtual.getId().equals(aviso.getId()))
                .ifPresent(fixadoAtual -> {
                    fixadoAtual.setFixadoNoTopo(false);
                    repository.save(fixadoAtual);
                });

        aviso.setFixadoNoTopo(true);
        return AvisoResponse.from(repository.save(aviso));
    }

    /** Remove o destaque - o aviso volta a ordenar só por data, como qualquer outro. Mesma
     * autorização de {@link #desativar}. */
    @Transactional
    public AvisoResponse desfixarNoTopo(ContextoAutenticado contexto, Integer id) {
        Aviso aviso =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aviso não encontrado: " + id));
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, aviso.getCondominio().getId());

        aviso.setFixadoNoTopo(false);
        return AvisoResponse.from(repository.save(aviso));
    }

    /** Administrador vê qualquer aviso; funcionário/morador só do PRÓPRIO condomínio - se
     * não bate, devolve "não encontrado" (404) em vez de "proibido" (403), pra não revelar
     * que o aviso existe em outro condomínio onde a pessoa não está logada. */
    private Aviso buscarEntidadeVisivelParaContexto(ContextoAutenticado contexto, Integer id) {
        Aviso aviso =
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aviso não encontrado: " + id));
        boolean podeVer = Autorizacao.ehAdministrador(contexto)
                || aviso.getCondominio().getId().equals(contexto.condominioId());
        if (!podeVer) {
            throw new ResourceNotFoundException("Aviso não encontrado: " + id);
        }
        return aviso;
    }
}
