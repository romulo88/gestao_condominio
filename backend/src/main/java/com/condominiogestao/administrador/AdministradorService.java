package com.condominiogestao.administrador;

import com.condominiogestao.administrador.dto.AdministradorCreateRequest;
import com.condominiogestao.administrador.dto.AdministradorResponse;
import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.Cpf;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.pessoa.Pessoa;
import com.condominiogestao.pessoa.PessoaRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diferente de Funcionario/Morador, criar um administrador exige que quem chama já
 * seja administrador (ver {@link #exigirAdministrador}) - não faz sentido deixar
 * qualquer funcionário/morador autenticado se autopromover a administrador global. O
 * PRIMEIRO administrador do sistema não passa por aqui: é inserido direto no banco (ver
 * HANDOFF.md), do mesmo jeito que o Romulo já tinha feito na tabela {@code pessoas}
 * antes desse papel existir.
 */
@Service
@Transactional(readOnly = true)
public class AdministradorService {

    private final AdministradorRepository repository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String senhaPadrao;

    public AdministradorService(
            AdministradorRepository repository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.senha-padrao}") String senhaPadrao) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaPadrao = senhaPadrao;
    }

    public List<AdministradorResponse> listar() {
        return repository.findAll().stream().map(AdministradorResponse::from).toList();
    }

    public AdministradorResponse buscarPorId(Integer id) {
        return AdministradorResponse.from(buscarEntidadePorId(id));
    }

    @Transactional
    public AdministradorResponse criar(ContextoAutenticado contexto, AdministradorCreateRequest request) {
        Autorizacao.exigirAdministrador(contexto);

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
            throw new ConflictException("Essa pessoa (CPF " + cpf + ") já é administrador");
        }

        Administrador administrador = new Administrador();
        administrador.setPessoa(pessoa);
        // situacao já nasce 'ativo' por default no campo da entidade

        return AdministradorResponse.from(repository.save(administrador));
    }

    private Administrador buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado: " + id));
    }
}
