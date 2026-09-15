package com.condominiogestao.funcionario;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.Cpf;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.funcionario.dto.FuncionarioCreateRequest;
import com.condominiogestao.funcionario.dto.FuncionarioResponse;
import com.condominiogestao.pessoa.Pessoa;
import com.condominiogestao.pessoa.PessoaFotoService;
import com.condominiogestao.pessoa.PessoaRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PessoaRepository pessoaRepository;
    private final PessoaFotoService pessoaFotoService;
    private final PasswordEncoder passwordEncoder;
    private final String senhaPadrao;

    public FuncionarioService(
            FuncionarioRepository repository,
            PessoaRepository pessoaRepository,
            PessoaFotoService pessoaFotoService,
            PasswordEncoder passwordEncoder,
            @Value("${auth.senha-padrao}") String senhaPadrao) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.pessoaFotoService = pessoaFotoService;
        this.passwordEncoder = passwordEncoder;
        this.senhaPadrao = senhaPadrao;
    }

    public List<FuncionarioResponse> listar() {
        return repository.findAll().stream()
                .map(f -> FuncionarioResponse.from(f, pessoaFotoService.buscarUrl(f.getId())))
                .toList();
    }

    public FuncionarioResponse buscarPorId(Integer id) {
        Funcionario funcionario = buscarEntidadePorId(id);
        return FuncionarioResponse.from(funcionario, pessoaFotoService.buscarUrl(funcionario.getId()));
    }

    /** 404 quando o CPF existe como {@link Pessoa} mas ainda não tem papel de funcionário
     * (ou não existe pessoa nenhuma) - usado pra evitar tentar recriar um funcionário
     * que já existe (ex: aba Funcionário do cadastro de condomínio). */
    public FuncionarioResponse buscarPorCpf(String cpf) {
        Funcionario funcionario = repository
                .findByPessoaCpf(Cpf.normalizar(cpf))
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum funcionário com o CPF " + cpf));
        return FuncionarioResponse.from(funcionario, pessoaFotoService.buscarUrl(funcionario.getId()));
    }

    /** Sobe (ou substitui) a foto de perfil - fica na {@link Pessoa} por trás do
     * funcionário, então vale pra qualquer outro papel que essa mesma pessoa tenha. */
    @Transactional
    public FuncionarioResponse atualizarFoto(Integer id, MultipartFile arquivo) {
        Funcionario funcionario = buscarEntidadePorId(id);
        String fotoUrl = pessoaFotoService.atualizar(id, arquivo);
        return FuncionarioResponse.from(funcionario, fotoUrl);
    }

    @Transactional
    public FuncionarioResponse removerFoto(Integer id) {
        Funcionario funcionario = buscarEntidadePorId(id);
        pessoaFotoService.remover(id);
        return FuncionarioResponse.from(funcionario, null);
    }

    /** Chave bruta da foto (ver `PessoaFotoService.chave`) - usada por `GET
     * /api/funcionarios/{id}/foto`, que serve a imagem direto em vez de devolver um link
     * assinado do MinIO. */
    public String chaveFoto(Integer id) {
        return pessoaFotoService.chave(id);
    }

    /**
     * Reaproveita a {@link Pessoa} existente com esse CPF (ex: já é morador e agora
     * também vira funcionário) ou cria uma nova. Se a pessoa já existir sem e-mail
     * cadastrado, preenche com o e-mail informado aqui; se já tiver um, o e-mail
     * existente prevalece (não sobrescreve silenciosamente).
     */
    @Transactional
    public FuncionarioResponse criar(FuncionarioCreateRequest request) {
        String cpf = Cpf.normalizar(request.cpf());
        Pessoa pessoa = pessoaRepository.findByCpf(cpf).orElseGet(() -> {
            Pessoa nova = new Pessoa();
            nova.setNome(request.nome());
            nova.setCpf(cpf);
            nova.setEmail(request.email());
            // Nasce com a senha padrão, marcada pra trocar no primeiro acesso - ver
            // AuthService (fluxo de "Esqueci minha senha").
            nova.setSenhaHash(passwordEncoder.encode(senhaPadrao));
            nova.setPrecisaTrocarSenha(true);
            return pessoaRepository.save(nova);
        });

        if (pessoa.getEmail() == null && request.email() != null) {
            pessoa.setEmail(request.email());
        }

        if (repository.existsById(pessoa.getId())) {
            throw new ConflictException("Essa pessoa (CPF " + request.cpf() + ") já é funcionário");
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setPessoa(pessoa);
        // situacao já nasce 'ativo' por default no campo da entidade

        Funcionario salvo = repository.save(funcionario);
        // Pessoa reaproveitada (já era morador, por exemplo) pode já ter foto - recém-criada nunca tem.
        return FuncionarioResponse.from(salvo, pessoaFotoService.buscarUrl(salvo.getId()));
    }

    private Funcionario buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }
}
