package com.condominiogestao.morador;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Bloco;
import com.condominiogestao.condominio.BlocoRepository;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.morador.dto.MoradorCondominioCreateRequest;
import com.condominiogestao.morador.dto.MoradorCondominioResponse;
import com.condominiogestao.morador.dto.MoradorCondominioResumoResponse;
import com.condominiogestao.morador.dto.MoradorCondominioUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MoradorCondominioService {

    private final MoradorCondominioRepository repository;
    private final MoradorRepository moradorRepository;
    private final CondominioRepository condominioRepository;
    private final BlocoRepository blocoRepository;
    private final PasswordEncoder passwordEncoder;
    private final String senhaPadrao;

    public MoradorCondominioService(
            MoradorCondominioRepository repository,
            MoradorRepository moradorRepository,
            CondominioRepository condominioRepository,
            BlocoRepository blocoRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.senha-padrao}") String senhaPadrao) {
        this.repository = repository;
        this.moradorRepository = moradorRepository;
        this.condominioRepository = condominioRepository;
        this.blocoRepository = blocoRepository;
        this.passwordEncoder = passwordEncoder;
        this.senhaPadrao = senhaPadrao;
    }

    /** Filtra por condomínio ou por morador se informados; sem nenhum, lista tudo. */
    public List<MoradorCondominioResponse> listar(Integer condominioId, Integer moradorId) {
        List<MoradorCondominio> vinculos;
        if (condominioId != null) {
            vinculos = repository.findByCondominioId(condominioId);
        } else if (moradorId != null) {
            vinculos = repository.findByMoradorId(moradorId);
        } else {
            vinculos = repository.findAll();
        }
        return vinculos.stream().map(MoradorCondominioResponse::from).toList();
    }

    /** Página da listagem do cadastro de condomínio - mesmo espírito de
     * {@code FuncionarioCondominioService.listarPaginaPorCondominio} (pedido do Romulo:
     * paginação de 15 registros, "a ideia é performance para não listar todos de vez").
     * Já vem com nome/CPF/e-mail embutidos (ver {@link MoradorCondominioResumoResponse}) -
     * elimina o {@code GET /api/moradores/{id}} por linha que a tela batia antes disso.
     * {@code busca} bate por nome (contém) OU pelos dígitos do CPF (contém), mesmo
     * critério de sempre. */
    public PaginaResponse<MoradorCondominioResumoResponse> listarPaginaPorCondominio(
            Integer condominioId, String busca, int pagina, int tamanho) {
        String buscaNormalizada = busca == null ? "" : busca.trim();
        String buscaNome = buscaNormalizada.isEmpty() ? null : "%" + buscaNormalizada.toLowerCase() + "%";
        String buscaDigitos = buscaNormalizada.replaceAll("[^0-9]", "");
        String buscaCpf = buscaDigitos.isEmpty() ? null : "%" + buscaDigitos + "%";

        Page<MoradorCondominio> paginaVinculos = repository.buscarPorCondominio(
                condominioId, buscaNome, buscaCpf, PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100)));

        return PaginaResponse.from(paginaVinculos.map(MoradorCondominioResumoResponse::from));
    }

    public MoradorCondominioResponse buscarPorId(Integer id) {
        return MoradorCondominioResponse.from(buscarEntidadePorId(id));
    }

    @Transactional
    public MoradorCondominioResponse criar(ContextoAutenticado contexto, MoradorCondominioCreateRequest request) {
        Autorizacao.exigirAdministradorOuGestor(contexto, request.condominioId());

        Morador morador = moradorRepository
                .findById(request.moradorId())
                .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + request.moradorId()));
        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        if (repository.existsByMoradorIdAndCondominioId(request.moradorId(), request.condominioId())) {
            throw new ConflictException("Esse morador já tem vínculo com esse condomínio");
        }

        Bloco bloco = null;
        if (request.blocoId() != null) {
            bloco = blocoRepository
                    .findById(request.blocoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bloco não encontrado: " + request.blocoId()));
            if (!bloco.getCondominio().getId().equals(request.condominioId())) {
                throw new InvalidRequestException(
                        "O bloco informado pertence a outro condomínio, não a " + request.condominioId());
            }
        }

        MoradorCondominio vinculo = new MoradorCondominio();
        vinculo.setMorador(morador);
        vinculo.setCondominio(condominio);
        vinculo.setBloco(bloco);
        vinculo.setNumeroUnidade(request.numeroUnidade());

        return MoradorCondominioResponse.from(repository.save(vinculo));
    }

    /** Só corrige bloco/unidade - mesma autorização de {@link #criar}. */
    @Transactional
    public MoradorCondominioResponse atualizar(ContextoAutenticado contexto, Integer id, MoradorCondominioUpdateRequest request) {
        MoradorCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        Bloco bloco = null;
        if (request.blocoId() != null) {
            bloco = blocoRepository
                    .findById(request.blocoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bloco não encontrado: " + request.blocoId()));
            if (!bloco.getCondominio().getId().equals(vinculo.getCondominio().getId())) {
                throw new InvalidRequestException(
                        "O bloco informado pertence a outro condomínio, não a " + vinculo.getCondominio().getId());
            }
        }

        vinculo.setBloco(bloco);
        vinculo.setNumeroUnidade(request.numeroUnidade());
        // Email mora em Pessoa (identidade compartilhada), não no vínculo - a entidade já
        // está no contexto de persistência (veio de buscarEntidadePorId nesta mesma
        // transação), então basta mutar; o Hibernate persiste sozinho no commit (mesmo
        // padrão já usado em FuncionarioService/MoradorService pro ramo "pessoa já existe").
        vinculo.getMorador().getPessoa().setEmail(request.email());

        return MoradorCondominioResponse.from(repository.save(vinculo));
    }

    /** Soft-delete de sempre - nunca apaga a linha, só marca inativo. Mesma autorização de
     * {@link #criar}. */
    @Transactional
    public MoradorCondominioResponse desativar(ContextoAutenticado contexto, Integer id) {
        MoradorCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        vinculo.setSituacao(Situacao.inativo);

        return MoradorCondominioResponse.from(repository.save(vinculo));
    }

    /** Reverte um {@link #desativar} - religa o vínculo sem mexer em mais nada. Mesma
     * autorização de {@link #criar}. */
    @Transactional
    public MoradorCondominioResponse ativar(ContextoAutenticado contexto, Integer id) {
        MoradorCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        vinculo.setSituacao(Situacao.ativo);

        return MoradorCondominioResponse.from(repository.save(vinculo));
    }

    /** Reseta a senha da pessoa por trás do vínculo pra padrão ({@code auth.senha-padrao})
     * e liga {@code precisaTrocarSenha} de novo - mesmo estado de uma pessoa recém-criada.
     * Pensado pro caso "morador esqueceu a senha atual (não só a nunca trocou)": quem tem
     * esse botão na mão (síndico/sub-síndico/administrador) precisa avisar o morador da
     * senha padrão por fora, já que ela não é devolvida em nenhum lugar visível da tela de
     * login (mesma ressalva de segurança do cadastro). Mesma autorização de {@link #criar}
     * - afeta a pessoa (login em qualquer condomínio), não só este vínculo, mas só quem
     * gerencia este condomínio consegue acionar por aqui. */
    @Transactional
    public MoradorCondominioResponse zerarSenha(ContextoAutenticado contexto, Integer id) {
        MoradorCondominio vinculo = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuGestor(contexto, vinculo.getCondominio().getId());

        var pessoa = vinculo.getMorador().getPessoa();
        pessoa.setSenhaHash(passwordEncoder.encode(senhaPadrao));
        pessoa.setPrecisaTrocarSenha(true);

        return MoradorCondominioResponse.from(vinculo);
    }

    private MoradorCondominio buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo morador-condomínio não encontrado: " + id));
    }
}
