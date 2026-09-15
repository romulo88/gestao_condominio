package com.condominiogestao.auth;

import com.condominiogestao.auth.dto.ContextoDto;
import com.condominiogestao.auth.dto.LoginRequest;
import com.condominiogestao.auth.dto.LoginResponse;
import com.condominiogestao.auth.dto.SelecionarContextoRequest;
import com.condominiogestao.auth.dto.TokenResponse;
import com.condominiogestao.auth.dto.TrocarSenhaRequest;
import com.condominiogestao.auth.dto.VerificarIdentidadeRequest;
import com.condominiogestao.administrador.AdministradorRepository;
import com.condominiogestao.common.Cpf;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.common.UnauthorizedException;
import com.condominiogestao.funcionario.FuncionarioCondominioRepository;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.morador.MoradorCondominioRepository;
import com.condominiogestao.morador.MoradorRepository;
import com.condominiogestao.pessoa.Pessoa;
import com.condominiogestao.pessoa.PessoaRepository;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.security.JwtService;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final PessoaRepository pessoaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioCondominioRepository funcionarioCondominioRepository;
    private final MoradorRepository moradorRepository;
    private final MoradorCondominioRepository moradorCondominioRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String senhaPadrao;

    public AuthService(
            PessoaRepository pessoaRepository,
            FuncionarioRepository funcionarioRepository,
            FuncionarioCondominioRepository funcionarioCondominioRepository,
            MoradorRepository moradorRepository,
            MoradorCondominioRepository moradorCondominioRepository,
            AdministradorRepository administradorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${auth.senha-padrao}") String senhaPadrao) {
        this.pessoaRepository = pessoaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.funcionarioCondominioRepository = funcionarioCondominioRepository;
        this.moradorRepository = moradorRepository;
        this.moradorCondominioRepository = moradorCondominioRepository;
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.senhaPadrao = senhaPadrao;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Pessoa pessoa = pessoaRepository
                .findByCpf(Cpf.normalizar(request.cpf()))
                .orElseThrow(() -> new UnauthorizedException("CPF ou senha inválidos"));

        if (pessoa.getSenhaHash() == null || !passwordEncoder.matches(request.senha(), pessoa.getSenhaHash())) {
            throw new UnauthorizedException("CPF ou senha inválidos");
        }

        // Toda pessoa nasce com a senha padrão e essa flag ligada (ver services de
        // criação) - só o fluxo guiado (verificarIdentidade + trocarSenha) desliga.
        // Bloqueia mesmo que a senha digitada bata certinho com a padrão.
        if (pessoa.isPrecisaTrocarSenha()) {
            throw new UnauthorizedException(
                    "Você precisa definir uma senha nova antes de entrar - use \"Esqueceu sua senha?\"");
        }

        List<ContextoDto> contextos = listarContextosAtivos(pessoa);
        if (contextos.isEmpty()) {
            throw new UnauthorizedException("Essa pessoa não tem nenhum vínculo ativo em condomínio algum");
        }

        // Guarda o valor ANTERIOR antes de sobrescrever - é isso que o frontend usa como
        // "desde quando" no alerta de mudança de status do morador (ver LoginResponse).
        // `trocarContexto` (trocar de perfil sem sair da sessão) não passa por aqui de
        // propósito - não é um login novo, é a mesma sessão com outro chapéu.
        LocalDateTime ultimoLoginAnterior = pessoa.getUltimoLogin();
        pessoa.setUltimoLogin(LocalDateTime.now());
        pessoaRepository.save(pessoa);

        if (contextos.size() == 1) {
            String token = jwtService.gerarTokenCompleto(pessoa, contextos.get(0));
            return new LoginResponse(pessoa.getId(), pessoa.getNome(), contextos, token, null, ultimoLoginAnterior);
        }

        String preAuthToken = jwtService.gerarTokenPreAuth(pessoa);
        return new LoginResponse(
                pessoa.getId(), pessoa.getNome(), contextos, null, preAuthToken, ultimoLoginAnterior);
    }

    public TokenResponse selecionarContexto(String preAuthToken, SelecionarContextoRequest request) {
        Claims claims = jwtService.validarPreAuth(preAuthToken);
        Integer pessoaId = Integer.valueOf(claims.getSubject());

        Pessoa pessoa = buscarPessoa(pessoaId);
        ContextoDto escolhido = encontrarContexto(pessoa, request);
        return new TokenResponse(jwtService.gerarTokenCompleto(pessoa, escolhido));
    }

    /**
     * Lista os contextos que a pessoa JÁ LOGADA pode assumir - usado pra montar o modal de troca
     * de perfil (trocar sem deslogar).
     */
    public List<ContextoDto> listarMeusContextos(ContextoAutenticado contexto) {
        return listarContextosAtivos(buscarPessoa(contexto.pessoaId()));
    }

    /**
     * Como {@link #selecionarContexto}, mas pra quem JÁ está logado (token completo) e quer
     * trocar de perfil sem passar pela tela de login de novo.
     */
    public TokenResponse trocarContexto(ContextoAutenticado contexto, SelecionarContextoRequest request) {
        Pessoa pessoa = buscarPessoa(contexto.pessoaId());
        ContextoDto escolhido = encontrarContexto(pessoa, request);
        return new TokenResponse(jwtService.gerarTokenCompleto(pessoa, escolhido));
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository
                .findById(pessoaId)
                .orElseThrow(() -> new UnauthorizedException("Pessoa não existe mais"));
    }

    private ContextoDto encontrarContexto(Pessoa pessoa, SelecionarContextoRequest request) {
        return listarContextosAtivos(pessoa).stream()
                .filter(c -> Objects.equals(c.condominioId(), request.condominioId())
                        && c.tipoPapel() == request.tipoPapel())
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException(
                        "Essa pessoa não tem esse vínculo ativo (condomínio " + request.condominioId() + ", "
                                + request.tipoPapel() + ")"));
    }

    /**
     * Passo 1 do fluxo "Esqueci minha senha" (ver {@code login/page.tsx}): só confirma
     * que existe uma {@link Pessoa} com esse CPF <b>e</b> esse e-mail (não diz qual dos
     * dois está errado, se algum estiver - reduz a chance de alguém usar isso pra
     * descobrir se um CPF existe no sistema). E-mail comparado sem diferenciar
     * maiúsculas/minúsculas.
     *
     * <p><b>Aviso de segurança</b>: CPF + e-mail digitados aqui não é prova real de
     * posse do e-mail (não tem link único enviado por e-mail ainda, ver
     * docs/modelo-dados.md) - é mais forte que só CPF, mas ainda não é definitivo.
     */
    public void verificarIdentidade(VerificarIdentidadeRequest request) {
        buscarPessoaPorCpfEEmail(request.cpf(), request.email());
    }

    /**
     * Passo 2 do fluxo "Esqueci minha senha" - confere CPF + e-mail (de novo, por
     * segurança - defesa em profundidade caso alguém chame isso direto sem passar pelo
     * passo 1) e a senha atual, troca pela nova e desliga {@code precisaTrocarSenha}
     * (é isso que libera o login de verdade - ver {@link #login}). A pessoa nunca pode
     * escolher a própria senha padrão como senha nova, senão "nunca entra com a senha
     * padrão" deixaria de valer se alguém escolhesse isso de propósito.
     */
    @Transactional
    public void trocarSenha(TrocarSenhaRequest request) {
        Pessoa pessoa = buscarPessoaPorCpfEEmail(request.cpf(), request.email());

        if (pessoa.getSenhaHash() == null || !passwordEncoder.matches(request.senhaAtual(), pessoa.getSenhaHash())) {
            throw new UnauthorizedException("Senha atual inválida");
        }
        if (request.novaSenha().equals(senhaPadrao)) {
            throw new InvalidRequestException("A nova senha não pode ser igual à senha padrão");
        }

        pessoa.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        pessoa.setPrecisaTrocarSenha(false);
        pessoaRepository.save(pessoa);
    }

    private Pessoa buscarPessoaPorCpfEEmail(String cpf, String email) {
        Pessoa pessoa = pessoaRepository
                .findByCpf(Cpf.normalizar(cpf))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CPF e e-mail não correspondem a nenhuma pessoa cadastrada"));
        if (pessoa.getEmail() == null || !pessoa.getEmail().equalsIgnoreCase(email.trim())) {
            throw new ResourceNotFoundException("CPF e e-mail não correspondem a nenhuma pessoa cadastrada");
        }
        return pessoa;
    }

    /**
     * Une os vínculos ativos de funcionário (com perfil preenchido - item 2.1), de
     * morador e o papel de administrador (se houver) dessa pessoa num só conjunto de
     * contextos escolhíveis. Confere também a situação GLOBAL de cada papel
     * (Funcionario.situacao/Morador.situacao/Administrador.situacao), não só a do vínculo.
     */
    private List<ContextoDto> listarContextosAtivos(Pessoa pessoa) {
        List<ContextoDto> contextos = new ArrayList<>();

        administradorRepository.findById(pessoa.getId()).ifPresent(administrador -> {
            if (administrador.getSituacao() == Situacao.ativo) {
                // Sem condomínio associado de propósito - administrador é global (ver migration).
                contextos.add(new ContextoDto(null, null, TipoPessoa.administrador, null));
            }
        });

        funcionarioRepository.findById(pessoa.getId()).ifPresent(funcionario -> {
            if (funcionario.getSituacao() == Situacao.ativo) {
                funcionarioCondominioRepository.findByFuncionarioId(pessoa.getId()).stream()
                        .filter(fc -> fc.getSituacao() == Situacao.ativo && fc.getPerfil() != null)
                        .forEach(fc -> contextos.add(new ContextoDto(
                                fc.getCondominio().getId(),
                                fc.getCondominio().getNome(),
                                TipoPessoa.funcionario,
                                fc.getPerfil())));
            }
        });

        moradorRepository.findById(pessoa.getId()).ifPresent(morador -> {
            if (morador.getSituacao() == Situacao.ativo) {
                moradorCondominioRepository.findByMoradorId(pessoa.getId()).stream()
                        .filter(mc -> mc.getSituacao() == Situacao.ativo)
                        .forEach(mc -> contextos.add(new ContextoDto(
                                mc.getCondominio().getId(), mc.getCondominio().getNome(), TipoPessoa.morador, null)));
            }
        });

        return contextos;
    }
}
