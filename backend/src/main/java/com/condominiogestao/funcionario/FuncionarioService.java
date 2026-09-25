package com.condominiogestao.funcionario;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.funcionario.dto.FuncionarioCreateRequest;
import com.condominiogestao.funcionario.dto.FuncionarioResponse;
import com.condominiogestao.notificacao.SenhaProvisoriaService;
import com.condominiogestao.pessoa.Pessoa;
import com.condominiogestao.pessoa.PessoaFotoService;
import com.condominiogestao.pessoa.PessoaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final PessoaRepository pessoaRepository;
    private final PessoaFotoService pessoaFotoService;
    private final SenhaProvisoriaService senhaProvisoriaService;

    public FuncionarioService(
            FuncionarioRepository repository,
            PessoaRepository pessoaRepository,
            PessoaFotoService pessoaFotoService,
            SenhaProvisoriaService senhaProvisoriaService) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.pessoaFotoService = pessoaFotoService;
        this.senhaProvisoriaService = senhaProvisoriaService;
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

    /** 404 quando o e-mail existe como {@link Pessoa} mas ainda não tem papel de
     * funcionário (ou não existe pessoa nenhuma) - usado pra evitar tentar recriar um
     * funcionário que já existe (ex: aba Funcionário do cadastro de condomínio). */
    public FuncionarioResponse buscarPorEmail(String email) {
        Funcionario funcionario = repository
                .findByPessoaEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum funcionário com o e-mail " + email));
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
     * Reaproveita a {@link Pessoa} existente com esse e-mail (ex: já é morador e agora
     * também vira funcionário) ou cria uma nova - pedido do Romulo (LGPD, v177): e-mail
     * assumiu o papel que CPF tinha antes como identificador de deduplicação. E-mail é
     * OPCIONAL pra funcionário (diferente de morador) - sem e-mail, não tem como saber se
     * a pessoa já existe, então sempre cria uma {@link Pessoa} nova (efeito colateral
     * aceito - ver HANDOFF.md). Se a pessoa já existir sem telefone cadastrado, preenche
     * com o telefone informado aqui; se já tiver um, o telefone existente prevalece (mesmo
     * padrão que e-mail já seguia).
     */
    @Transactional
    public FuncionarioResponse criar(FuncionarioCreateRequest request) {
        String email = temValor(request.email()) ? request.email().trim().toLowerCase() : null;
        Pessoa pessoa = (email != null ? pessoaRepository.findByEmail(email) : Optional.<Pessoa>empty())
                .orElseGet(() -> {
                    Pessoa nova = new Pessoa();
                    nova.setNome(request.nome());
                    nova.setEmail(email);
                    nova.setTelefone(request.telefone());
                    // Pedido do Romulo: manda um código temporário pro e-mail (se tiver) em vez de
                    // nascer com a senha padrão pública - mesmo mecanismo de "Esqueci minha senha"
                    // (ver SenhaProvisoriaService). Só se aplica a Pessoa GENUINAMENTE nova - quem
                    // já existe (reaproveitada abaixo) já tem senha própria, não é tocada aqui.
                    senhaProvisoriaService.prepararPrimeiroAcesso(nova);
                    return pessoaRepository.save(nova);
                });

        if (pessoa.getTelefone() == null && request.telefone() != null) {
            pessoa.setTelefone(request.telefone());
        }

        if (repository.existsById(pessoa.getId())) {
            throw new ConflictException("Essa pessoa já é funcionário");
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setPessoa(pessoa);
        // situacao já nasce 'ativo' por default no campo da entidade

        Funcionario salvo = repository.save(funcionario);
        // Pessoa reaproveitada (já era morador, por exemplo) pode já ter foto - recém-criada nunca tem.
        return FuncionarioResponse.from(salvo, pessoaFotoService.buscarUrl(salvo.getId()));
    }

    private boolean temValor(String texto) {
        return texto != null && !texto.isBlank();
    }

    private Funcionario buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }
}
