package com.condominiogestao.morador;

import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.Cpf;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.morador.dto.MoradorCreateRequest;
import com.condominiogestao.morador.dto.MoradorResponse;
import com.condominiogestao.notificacao.SenhaProvisoriaService;
import com.condominiogestao.pessoa.Pessoa;
import com.condominiogestao.pessoa.PessoaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regra do item 5.3 ("moradores devem ser cadastrados apenas por funcionários") ainda
 * não é aplicada aqui - depende da autenticação (ver HANDOFF.md), que ainda vai definir
 * quem está chamando este endpoint.
 */
@Service
@Transactional(readOnly = true)
public class MoradorService {

    private final MoradorRepository repository;
    private final PessoaRepository pessoaRepository;
    private final SenhaProvisoriaService senhaProvisoriaService;

    public MoradorService(
            MoradorRepository repository, PessoaRepository pessoaRepository, SenhaProvisoriaService senhaProvisoriaService) {
        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.senhaProvisoriaService = senhaProvisoriaService;
    }

    public List<MoradorResponse> listar() {
        return repository.findAll().stream().map(MoradorResponse::from).toList();
    }

    public MoradorResponse buscarPorId(Integer id) {
        return MoradorResponse.from(buscarEntidadePorId(id));
    }

    /** 404 quando o CPF existe como {@link Pessoa} mas ainda não tem papel de morador
     * (ou não existe pessoa nenhuma) - usado pra evitar tentar recriar um morador que já
     * existe (aba Moradores do cadastro de condomínio). */
    public MoradorResponse buscarPorCpf(String cpf) {
        return repository
                .findByPessoaCpf(Cpf.normalizar(cpf))
                .map(MoradorResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum morador com o CPF " + cpf));
    }

    /**
     * Reaproveita a {@link Pessoa} existente com esse CPF (ex: já é funcionário e agora
     * também vira morador) ou cria uma nova. E-mail é obrigatório pro papel de morador
     * (item 3.4): se a pessoa já existir sem e-mail, preenche com o informado aqui; se
     * já tiver um, o e-mail existente prevalece.
     */
    @Transactional
    public MoradorResponse criar(MoradorCreateRequest request) {
        String cpf = Cpf.normalizar(request.cpf());
        Pessoa pessoa = pessoaRepository.findByCpf(cpf).orElseGet(() -> {
            Pessoa nova = new Pessoa();
            nova.setNome(request.nome());
            nova.setCpf(cpf);
            nova.setEmail(request.email());
            // Pedido do Romulo: manda um código temporário pro e-mail em vez de nascer com
            // a senha padrão pública - mesmo mecanismo de "Esqueci minha senha" (ver
            // SenhaProvisoriaService). Só se aplica a Pessoa GENUINAMENTE nova - quem já
            // existe (reaproveitada abaixo) já tem senha própria, não é tocada aqui.
            senhaProvisoriaService.prepararPrimeiroAcesso(nova);
            return pessoaRepository.save(nova);
        });

        if (pessoa.getEmail() == null) {
            pessoa.setEmail(request.email());
        }

        if (repository.existsById(pessoa.getId())) {
            throw new ConflictException("Essa pessoa (CPF " + request.cpf() + ") já é morador");
        }

        Morador morador = new Morador();
        morador.setPessoa(pessoa);
        // situacao já nasce 'ativo' por default no campo da entidade

        return MoradorResponse.from(repository.save(morador));
    }

    private Morador buscarEntidadePorId(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id));
    }
}
