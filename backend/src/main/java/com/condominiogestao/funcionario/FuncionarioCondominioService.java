package com.condominiogestao.funcionario;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.funcionario.dto.FuncionarioCondominioCreateRequest;
import com.condominiogestao.funcionario.dto.FuncionarioCondominioResponse;
import com.condominiogestao.funcionario.dto.FuncionarioCondominioResumoResponse;
import com.condominiogestao.funcionario.dto.FuncionarioCondominioUpdateRequest;
import com.condominiogestao.pessoa.PessoaFotoService;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FuncionarioCondominioService {

    private final FuncionarioCondominioRepository repository;
    private final FuncionarioRepository funcionarioRepository;
    private final CondominioRepository condominioRepository;
    private final PessoaFotoService pessoaFotoService;
    private final PasswordEncoder passwordEncoder;
    private final String senhaPadrao;

    public FuncionarioCondominioService(
            FuncionarioCondominioRepository repository,
            FuncionarioRepository funcionarioRepository,
            CondominioRepository condominioRepository,
            PessoaFotoService pessoaFotoService,
            PasswordEncoder passwordEncoder,
            @Value("${auth.senha-padrao}") String senhaPadrao) {
        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
        this.condominioRepository = condominioRepository;
        this.pessoaFotoService = pessoaFotoService;
        this.passwordEncoder = passwordEncoder;
        this.senhaPadrao = senhaPadrao;
    }

    /** Filtra por condomínio ou por funcionário se informados; sem nenhum, lista tudo. */
    public List<FuncionarioCondominioResponse> listar(Integer condominioId, Integer funcionarioId) {
        List<FuncionarioCondominio> vinculos;
        if (condominioId != null) {
            vinculos = repository.findByCondominioId(condominioId);
        } else if (funcionarioId != null) {
            vinculos = repository.findByFuncionarioId(funcionarioId);
        } else {
            vinculos = repository.findAll();
        }
        return vinculos.stream().map(FuncionarioCondominioResponse::from).toList();
    }

    /** Página da listagem do cadastro de condomínio - pedido do Romulo: "criar uma
     * paginação de 15 registros... a ideia é performance para não listar todos de vez".
     * Já vem com nome/CPF/e-mail/foto embutidos (ver {@link FuncionarioCondominioResumoResponse})
     * - a tela batia um {@code GET /api/funcionarios/{id}} por linha antes disso, só pra
     * pegar esses mesmos dados (N+1 de verdade: 1 chamada da lista + uma por funcionário -
     * com 15 por página, no máximo 15 dessas, mas o ideal mesmo é zero, e {@code
     * pessoaFotoService.buscarUrls} já resolve as fotos da página inteira numa consulta
     * só, não uma por pessoa). {@code busca} é o texto livre da caixa de busca da tela -
     * bate por nome (contém, sem diferenciar maiúscula/minúscula) OU pelos dígitos do CPF
     * (contém), o mesmo critério que a tela já usava do lado do cliente antes de existir
     * paginação de verdade. */
    public PaginaResponse<FuncionarioCondominioResumoResponse> listarPaginaPorCondominio(
            Integer condominioId, String busca, int pagina, int tamanho) {
        String buscaNormalizada = busca == null ? "" : busca.trim();
        String buscaNome = buscaNormalizada.isEmpty() ? null : "%" + buscaNormalizada.toLowerCase() + "%";
        String buscaDigitos = buscaNormalizada.replaceAll("[^0-9]", "");
        String buscaCpf = buscaDigitos.isEmpty() ? null : "%" + buscaDigitos + "%";

        Page<FuncionarioCondominio> paginaVinculos = repository.buscarPorCondominio(
                condominioId, buscaNome, buscaCpf, PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100)));

        List<Integer> idsFuncionarios = paginaVinculos.getContent().stream()
                .map(vinculo -> vinculo.getFuncionario().getId())
                .toList();
        Map<Integer, String> fotosPorFuncionarioId = pessoaFotoService.buscarUrls(idsFuncionarios);

        return PaginaResponse.from(paginaVinculos.map(
                vinculo -> FuncionarioCondominioResumoResponse.from(vinculo, fotosPorFuncionarioId.get(vinculo.getFuncionario().getId()))));
    }

    public FuncionarioCondominioResponse buscarPorId(Integer id) {
        return FuncionarioCondominioResponse.from(buscarEntidadePorId(id));
    }

    @Transactional
    public FuncionarioCondominioResponse criar(ContextoAutenticado contexto, FuncionarioCondominioCreateRequest request) {
        Autorizacao.exigirAdministradorOuGestor(contexto, request.condominioId());

        Funcionario funcionario = funcionarioRepository
                .findById(request.funcionarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + request.funcionarioId()));
        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        if (repository.existsByFuncionarioIdAndCondominioId(request.funcionarioId(), request.condominioId())) {
            throw new ConflictException("Esse funcionário já tem vínculo com esse condomínio");
        }

        // E-mail só é obrigatório pra quem tem perfil (acesso ao sistema) - é o que
        // identifica a pessoa no fluxo de "Esqueci minha senha" (pedido do Romulo:
        // "sem perfil" não deve exigir e-mail, mesmo o campo sendo opcional no banco).
        if (request.perfil() != null && semEmail(funcionario)) {
            throw new InvalidRequestException("Funcionário com perfil precisa ter e-mail cadastrado");
        }

        FuncionarioCondominio vinculo = new FuncionarioCondominio();
        vinculo.setFuncionario(funcionario);
        vinculo.setCondominio(condominio);
        vinculo.setPerfil(request.perfil());
        vinculo.setFuncao(request.funcao());

        return FuncionarioCondominioResponse.from(repository.save(vinculo));
    }

    /** Só corrige perfil/e-mail - mesma autorização de {@link #criar}. */
    @Transactional
    public FuncionarioCondominioResponse atualizar(
            ContextoAutenticado contexto, Integer id, FuncionarioCondominioUpdateRequest request) {
        FuncionarioCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        boolean informouEmail = request.email() != null && !request.email().isBlank();
        if (request.perfil() != null && !informouEmail && semEmail(vinculo.getFuncionario())) {
            throw new InvalidRequestException("Funcionário com perfil precisa ter e-mail cadastrado");
        }

        vinculo.setPerfil(request.perfil());
        vinculo.setFuncao(request.funcao());
        // Email mora em Pessoa (identidade compartilhada), não no vínculo - mesmo padrão
        // já usado em MoradorCondominioService.atualizar. Só sobrescreve quando um valor
        // de verdade foi informado - em branco não apaga o e-mail que já existia (a mesma
        // Pessoa pode ter outros papéis/vínculos usando esse e-mail).
        if (informouEmail) {
            vinculo.getFuncionario().getPessoa().setEmail(request.email());
        }

        return FuncionarioCondominioResponse.from(repository.save(vinculo));
    }

    private boolean semEmail(Funcionario funcionario) {
        String email = funcionario.getPessoa().getEmail();
        return email == null || email.isBlank();
    }

    /** Soft-delete de sempre - nunca apaga a linha, só marca inativo. Mesma autorização de
     * {@link #criar}. */
    @Transactional
    public FuncionarioCondominioResponse desativar(ContextoAutenticado contexto, Integer id) {
        FuncionarioCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        vinculo.setSituacao(Situacao.inativo);

        return FuncionarioCondominioResponse.from(repository.save(vinculo));
    }

    /** Reverte um {@link #desativar} - religa o vínculo sem mexer em mais nada. Mesma
     * autorização de {@link #criar}. */
    @Transactional
    public FuncionarioCondominioResponse ativar(ContextoAutenticado contexto, Integer id) {
        FuncionarioCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        vinculo.setSituacao(Situacao.ativo);

        return FuncionarioCondominioResponse.from(repository.save(vinculo));
    }

    /** Reseta a senha da pessoa por trás do vínculo pra padrão ({@code auth.senha-padrao})
     * e liga {@code precisaTrocarSenha} de novo - mesmo padrão de
     * {@code MoradorCondominioService.zerarSenha}, mesma justificativa/ressalva de
     * segurança lá documentada. Mesma autorização de {@link #criar}. */
    @Transactional
    public FuncionarioCondominioResponse zerarSenha(ContextoAutenticado contexto, Integer id) {
        FuncionarioCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        var pessoa = vinculo.getFuncionario().getPessoa();
        pessoa.setSenhaHash(passwordEncoder.encode(senhaPadrao));
        pessoa.setPrecisaTrocarSenha(true);

        return FuncionarioCondominioResponse.from(vinculo);
    }

    private FuncionarioCondominio buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo funcionário-condomínio não encontrado: " + id));
    }
}
