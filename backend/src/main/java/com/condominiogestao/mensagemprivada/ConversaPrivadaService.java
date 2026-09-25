package com.condominiogestao.mensagemprivada;

import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioCondominioRepository;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.mensagemprivada.dto.CandidatoDestinatarioResponse;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaCreateRequest;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaDestinatarioResponse;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaDetalheResponse;
import com.condominiogestao.mensagemprivada.dto.ConversaPrivadaResumoResponse;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaCreateRequest;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaDocumentoResponse;
import com.condominiogestao.mensagemprivada.dto.MensagemPrivadaResponse;
import com.condominiogestao.morador.Morador;
import com.condominiogestao.morador.MoradorCondominio;
import com.condominiogestao.morador.MoradorCondominioRepository;
import com.condominiogestao.morador.MoradorRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mensagem privada (pedido do Romulo) - conversa livre entre um morador OU funcionário
 * (autor) e um ou mais funcionários com login do mesmo condomínio (destinatários). Privacidade
 * estrita: só quem participa (autor + destinatários) enxerga - diferente do sigilo de
 * demanda, aqui nem síndico/sub-síndico veem por padrão (decidido explicitamente assim,
 * ver HANDOFF.md).
 *
 * <p>"Pendente" (destaque vermelho no ícone do menu) pra um viewer = existe {@link
 * MensagemPrivada} numa conversa que ele participa, escrita por OUTRO participante, depois
 * da última vez que ELE viu aquela conversa (ver {@link #calcularPendente}) - substitui o
 * flag "lida" por mensagem (que fazia sentido pra pergunta+resposta única, não pra chat).
 */
@Service
@Transactional(readOnly = true)
public class ConversaPrivadaService {

    private final ConversaPrivadaRepository repository;
    private final ConversaPrivadaDestinatarioRepository destinatarioRepository;
    private final MensagemPrivadaRepository mensagemRepository;
    private final MensagemPrivadaDocumentoRepository documentoRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorRepository moradorRepository;
    private final MoradorCondominioRepository moradorCondominioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioCondominioRepository funcionarioCondominioRepository;

    public ConversaPrivadaService(
            ConversaPrivadaRepository repository,
            ConversaPrivadaDestinatarioRepository destinatarioRepository,
            MensagemPrivadaRepository mensagemRepository,
            MensagemPrivadaDocumentoRepository documentoRepository,
            CondominioRepository condominioRepository,
            MoradorRepository moradorRepository,
            MoradorCondominioRepository moradorCondominioRepository,
            FuncionarioRepository funcionarioRepository,
            FuncionarioCondominioRepository funcionarioCondominioRepository) {
        this.repository = repository;
        this.destinatarioRepository = destinatarioRepository;
        this.mensagemRepository = mensagemRepository;
        this.documentoRepository = documentoRepository;
        this.condominioRepository = condominioRepository;
        this.moradorRepository = moradorRepository;
        this.moradorCondominioRepository = moradorCondominioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.funcionarioCondominioRepository = funcionarioCondominioRepository;
    }

    /** Funcionários COM LOGIN (perfil preenchido) e ativos do condomínio de quem está
     * logado - alimenta a combo de busca de destinatário (por nome), mesmo espírito de
     * {@code DemandaResponsavelService.listarCandidatos}, com o filtro extra de perfil. */
    public List<CandidatoDestinatarioResponse> listarCandidatos(ContextoAutenticado contexto) {
        return funcionarioCondominioRepository.findByCondominioId(contexto.condominioId()).stream()
                .filter(v -> v.getPerfil() != null
                        && v.getSituacao() == Situacao.ativo
                        && v.getFuncionario().getSituacao() == Situacao.ativo)
                .map(v -> new CandidatoDestinatarioResponse(v.getFuncionario().getId(), v.getFuncionario().getNome(), v.getPerfil()))
                .sorted(Comparator.comparing(CandidatoDestinatarioResponse::nome))
                .toList();
    }

    /** Minhas conversas (autor OU destinatário, conforme o papel de quem está logado) -
     * mais recente primeiro, paginado (pedido do Romulo: "mesma quantidade da listagem de
     * demandas" - 20 por página, mesmo espírito de {@code DemandaService.listarPagina}:
     * pagina em memória sobre a lista já carregada, não via {@code Pageable} do Spring -
     * o volume de conversa por pessoa não justifica paginar no banco). */
    public PaginaResponse<ConversaPrivadaResumoResponse> listarPagina(ContextoAutenticado contexto, int pagina, int tamanho) {
        List<ConversaPrivada> conversas = ehFuncionario(contexto)
                ? repository.buscarParaFuncionario(contexto.condominioId(), contexto.pessoaId())
                : repository.findByCondominioIdAndMoradorAutorIdOrderByUpdatedAtDesc(
                        contexto.condominioId(), contexto.pessoaId());

        int totalItens = conversas.size();
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), 100);
        int totalPaginas = (int) Math.ceil(totalItens / (double) tamanhoSeguro);
        int paginaSegura = Math.max(pagina, 0);
        int inicio = Math.min(paginaSegura * tamanhoSeguro, totalItens);
        int fim = Math.min(inicio + tamanhoSeguro, totalItens);
        List<ConversaPrivada> conversasDaPagina = conversas.subList(inicio, fim);
        if (conversasDaPagina.isEmpty()) {
            return new PaginaResponse<>(List.of(), paginaSegura, totalPaginas, totalItens);
        }

        List<Integer> ids = conversasDaPagina.stream().map(ConversaPrivada::getId).toList();
        Map<Integer, List<MensagemPrivada>> mensagensPorConversa = agruparPorConversa(mensagemRepository.findByConversaIdIn(ids));
        Map<Integer, List<ConversaPrivadaDestinatario>> destinatariosPorConversa =
                agruparDestinatariosPorConversa(destinatarioRepository.findByConversaIdIn(ids));
        Map<Integer, MoradorCondominio> vinculoPorMorador = vinculoPorMoradorNoCondominio(contexto.condominioId());

        List<ConversaPrivadaResumoResponse> itens = conversasDaPagina.stream()
                .map(conversa -> montarResumo(
                        contexto,
                        conversa,
                        mensagensPorConversa.getOrDefault(conversa.getId(), List.of()),
                        destinatariosPorConversa.getOrDefault(conversa.getId(), List.of()),
                        vinculoPorMorador))
                .toList();
        return new PaginaResponse<>(itens, paginaSegura, totalPaginas, totalItens);
    }

    /** Vínculo (bloco/unidade) de cada morador do condomínio, numa consulta só - reaproveita
     * {@code MoradorCondominioRepository.findByCondominioId} (já existente pro cadastro) em
     * vez de criar uma consulta nova por morador, evitando N+1 na listagem. */
    private Map<Integer, MoradorCondominio> vinculoPorMoradorNoCondominio(Integer condominioId) {
        Map<Integer, MoradorCondominio> mapa = new HashMap<>();
        for (MoradorCondominio vinculo : moradorCondominioRepository.findByCondominioId(condominioId)) {
            mapa.put(vinculo.getMorador().getId(), vinculo);
        }
        return mapa;
    }

    /** Detalhe (chat completo) - marca a conversa como vista por quem está abrindo, na
     * mesma tacada (pedido do Romulo é o destaque vermelho desaparecer quando o funcionário/
     * morador de fato abre e lê, mesmo espírito de um chat comum, sem precisar de um botão
     * "marcar como lida" separado). */
    @Transactional
    public ConversaPrivadaDetalheResponse buscarDetalhe(ContextoAutenticado contexto, Integer id) {
        ConversaPrivada conversa = buscarConversa(id);
        exigirParticipante(contexto, conversa);
        marcarComoVista(contexto, conversa);

        List<MensagemPrivada> mensagens = mensagemRepository.findByConversaIdOrderByCreatedAtAsc(id);
        List<ConversaPrivadaDestinatario> destinatarios = destinatarioRepository.findByConversaId(id);
        Map<Integer, List<MensagemPrivadaDocumentoResponse>> anexosPorMensagem = agruparAnexosPorMensagem(
                mensagens.stream().map(MensagemPrivada::getId).toList());

        return new ConversaPrivadaDetalheResponse(
                conversa.getId(),
                autorTipo(conversa),
                autorNome(conversa),
                destinatarios.stream().map(ConversaPrivadaDestinatarioResponse::from).toList(),
                mensagens.stream()
                        .map(m -> MensagemPrivadaResponse.from(
                                m, ehAutorDaMensagem(contexto, m), anexosPorMensagem.getOrDefault(m.getId(), List.of())))
                        .toList(),
                conversa.getCreatedAt());
    }

    /** Cria a conversa + a primeira mensagem, endereçada a 1 ou mais funcionários com
     * login do mesmo condomínio de quem está criando. */
    @Transactional
    public ConversaPrivadaDetalheResponse criar(ContextoAutenticado contexto, ConversaPrivadaCreateRequest request) {
        Condominio condominio = condominioRepository
                .findById(contexto.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + contexto.condominioId()));

        Set<Integer> idsUnicos = new LinkedHashSet<>(request.destinatariosId());
        List<Funcionario> destinatarios = idsUnicos.stream().map(this::buscarDestinatarioValido).toList();

        ConversaPrivada conversa = new ConversaPrivada();
        conversa.setCondominio(condominio);
        if (ehFuncionario(contexto)) {
            Funcionario autor = buscarFuncionario(contexto.pessoaId());
            if (destinatarios.stream().anyMatch(d -> d.getId().equals(autor.getId()))) {
                throw new InvalidRequestException("Você não pode endereçar uma mensagem privada a você mesmo");
            }
            conversa.setFuncionarioAutor(autor);
        } else {
            conversa.setMoradorAutor(buscarMorador(contexto.pessoaId()));
        }
        conversa.setAutorUltimaVisualizacaoEm(LocalDateTime.now());
        conversa = repository.save(conversa);

        for (Funcionario destinatario : destinatarios) {
            ConversaPrivadaDestinatario vinculo = new ConversaPrivadaDestinatario();
            vinculo.setConversa(conversa);
            vinculo.setFuncionario(destinatario);
            destinatarioRepository.save(vinculo);
        }

        criarMensagem(contexto, conversa, request.texto());

        return buscarDetalhe(contexto, conversa.getId());
    }

    /** Qualquer participante (autor original ou qualquer destinatário) pode escrever -
     * "conversa livre" (pedido do Romulo). */
    @Transactional
    public MensagemPrivadaResponse enviarMensagem(ContextoAutenticado contexto, MensagemPrivadaCreateRequest request) {
        ConversaPrivada conversa = buscarConversa(request.conversaId());
        exigirParticipante(contexto, conversa);

        MensagemPrivada mensagem = criarMensagem(contexto, conversa, request.texto());
        marcarComoVista(contexto, conversa);
        conversa.setUpdatedAt(LocalDateTime.now());
        repository.save(conversa);

        return MensagemPrivadaResponse.from(mensagem, true, List.of());
    }

    /** Boolean simples pra alimentar o ícone do menu (destaque vermelho) sem precisar
     * carregar a listagem inteira - mesmo espírito de {@code DemandaService.temNotaPendente},
     * só que aqui pros dois papéis (morador também espera resposta, agora que é chat). */
    public boolean existePendencia(ContextoAutenticado contexto) {
        List<ConversaPrivada> conversas = ehFuncionario(contexto)
                ? repository.buscarParaFuncionario(contexto.condominioId(), contexto.pessoaId())
                : repository.findByCondominioIdAndMoradorAutorIdOrderByUpdatedAtDesc(
                        contexto.condominioId(), contexto.pessoaId());
        if (conversas.isEmpty()) {
            return false;
        }

        List<Integer> ids = conversas.stream().map(ConversaPrivada::getId).toList();
        Map<Integer, List<MensagemPrivada>> mensagensPorConversa = agruparPorConversa(mensagemRepository.findByConversaIdIn(ids));
        Map<Integer, List<ConversaPrivadaDestinatario>> destinatariosPorConversa =
                agruparDestinatariosPorConversa(destinatarioRepository.findByConversaIdIn(ids));

        return conversas.stream().anyMatch(conversa -> calcularPendente(
                contexto,
                conversa,
                mensagensPorConversa.getOrDefault(conversa.getId(), List.of()),
                destinatariosPorConversa.getOrDefault(conversa.getId(), List.of())));
    }

    /** Busca a conversa exigindo que quem está logado participe dela - usado por {@link
     * MensagemPrivadaDocumentoService} pra checar visibilidade antes de anexar/remover/
     * baixar uma foto, mesmo espírito de {@code DemandaDocumentoService} dependendo de
     * {@code DemandaService.podeVer}. */
    public ConversaPrivada buscarConversaParticipante(ContextoAutenticado contexto, Integer conversaId) {
        ConversaPrivada conversa = buscarConversa(conversaId);
        exigirParticipante(contexto, conversa);
        return conversa;
    }

    private MensagemPrivada criarMensagem(ContextoAutenticado contexto, ConversaPrivada conversa, String texto) {
        MensagemPrivada mensagem = new MensagemPrivada();
        mensagem.setConversa(conversa);
        if (ehFuncionario(contexto)) {
            mensagem.setFuncionarioAutor(buscarFuncionario(contexto.pessoaId()));
        } else {
            mensagem.setMoradorAutor(buscarMorador(contexto.pessoaId()));
        }
        mensagem.setTexto(texto);
        return mensagemRepository.save(mensagem);
    }

    /** Atualiza o "visto por último" de quem está logado - autor usa a coluna na própria
     * conversa, destinatário usa a linha dele em {@link ConversaPrivadaDestinatario}. */
    private void marcarComoVista(ContextoAutenticado contexto, ConversaPrivada conversa) {
        LocalDateTime agora = LocalDateTime.now();
        if (ehAutor(contexto, conversa)) {
            conversa.setAutorUltimaVisualizacaoEm(agora);
            repository.save(conversa);
            return;
        }
        destinatarioRepository.findByConversaIdAndFuncionarioId(conversa.getId(), contexto.pessoaId())
                .ifPresent(destinatario -> {
                    destinatario.setUltimaVisualizacaoEm(agora);
                    destinatarioRepository.save(destinatario);
                });
    }

    private ConversaPrivadaResumoResponse montarResumo(
            ContextoAutenticado contexto,
            ConversaPrivada conversa,
            List<MensagemPrivada> mensagens,
            List<ConversaPrivadaDestinatario> destinatarios,
            Map<Integer, MoradorCondominio> vinculoPorMorador) {
        MensagemPrivada ultima = mensagens.stream().max(Comparator.comparing(MensagemPrivada::getCreatedAt)).orElse(null);
        MoradorCondominio vinculoAutor =
                conversa.getMoradorAutor() != null ? vinculoPorMorador.get(conversa.getMoradorAutor().getId()) : null;
        return new ConversaPrivadaResumoResponse(
                conversa.getId(),
                autorTipo(conversa),
                autorNome(conversa),
                vinculoAutor != null && vinculoAutor.getBloco() != null ? vinculoAutor.getBloco().getNome() : null,
                vinculoAutor != null ? vinculoAutor.getNumeroUnidade() : null,
                destinatarios.stream().map(ConversaPrivadaDestinatarioResponse::from).toList(),
                ultima != null ? ultima.getTexto() : null,
                ultima != null ? nomeAutorMensagem(ultima) : null,
                ultima != null ? ultima.getCreatedAt() : conversa.getCreatedAt(),
                calcularPendente(contexto, conversa, mensagens, destinatarios),
                conversa.getCreatedAt());
    }

    /** Existe mensagem de outro participante mais nova que a última visualização de quem
     * está vendo (ou nunca visualizada) - ver Javadoc da classe. */
    private boolean calcularPendente(
            ContextoAutenticado contexto,
            ConversaPrivada conversa,
            List<MensagemPrivada> mensagens,
            List<ConversaPrivadaDestinatario> destinatarios) {
        LocalDateTime ultimaVisualizacao = ultimaVisualizacaoDoViewer(contexto, conversa, destinatarios);
        return mensagens.stream()
                .anyMatch(m -> !ehAutorDaMensagem(contexto, m)
                        && (ultimaVisualizacao == null || m.getCreatedAt().isAfter(ultimaVisualizacao)));
    }

    private LocalDateTime ultimaVisualizacaoDoViewer(
            ContextoAutenticado contexto, ConversaPrivada conversa, List<ConversaPrivadaDestinatario> destinatarios) {
        if (ehAutor(contexto, conversa)) {
            return conversa.getAutorUltimaVisualizacaoEm();
        }
        // `findFirst()` direto sobre `.map(getUltimaVisualizacaoEm)` quebra com NPE quando
        // o destinatário ainda nunca visualizou (coluna null) - `Stream.findFirst()` usa
        // `Optional.of(...)` internamente, que não aceita null, mesmo vindo de um elemento
        // real do stream. Por isso o `findFirst()` primeiro (sobre a ENTIDADE, nunca null)
        // e só depois o `.map()` no Optional (esse sim é null-safe) pra extrair o campo.
        return destinatarios.stream()
                .filter(d -> d.getFuncionario().getId().equals(contexto.pessoaId()))
                .findFirst()
                .map(ConversaPrivadaDestinatario::getUltimaVisualizacaoEm)
                .orElse(null);
    }

    private boolean ehAutorDaMensagem(ContextoAutenticado contexto, MensagemPrivada mensagem) {
        if (ehFuncionario(contexto)) {
            return mensagem.getFuncionarioAutor() != null && mensagem.getFuncionarioAutor().getId().equals(contexto.pessoaId());
        }
        return mensagem.getMoradorAutor() != null && mensagem.getMoradorAutor().getId().equals(contexto.pessoaId());
    }

    private String nomeAutorMensagem(MensagemPrivada mensagem) {
        return mensagem.getMoradorAutor() != null
                ? mensagem.getMoradorAutor().getNome()
                : mensagem.getFuncionarioAutor().getNome();
    }

    private boolean ehAutor(ContextoAutenticado contexto, ConversaPrivada conversa) {
        if (ehFuncionario(contexto)) {
            return conversa.getFuncionarioAutor() != null && conversa.getFuncionarioAutor().getId().equals(contexto.pessoaId());
        }
        return conversa.getMoradorAutor() != null && conversa.getMoradorAutor().getId().equals(contexto.pessoaId());
    }

    private String autorTipo(ConversaPrivada conversa) {
        return conversa.getMoradorAutor() != null ? "morador" : "funcionario";
    }

    private String autorNome(ConversaPrivada conversa) {
        return conversa.getMoradorAutor() != null ? conversa.getMoradorAutor().getNome() : conversa.getFuncionarioAutor().getNome();
    }

    /** Só participante (autor ou destinatário) enxerga - privacidade estrita, nem síndico
     * por padrão (decisão explícita do Romulo, diferente do sigilo de demanda). */
    private void exigirParticipante(ContextoAutenticado contexto, ConversaPrivada conversa) {
        if (!conversa.getCondominio().getId().equals(contexto.condominioId())) {
            throw new ForbiddenException("Essa conversa não é do seu condomínio");
        }
        boolean participante = ehAutor(contexto, conversa)
                || (ehFuncionario(contexto)
                        && destinatarioRepository.existsByConversaIdAndFuncionarioId(conversa.getId(), contexto.pessoaId()));
        if (!participante) {
            throw new ForbiddenException("Você não participa dessa conversa");
        }
    }

    private Funcionario buscarDestinatarioValido(Integer funcionarioId) {
        Funcionario funcionario = funcionarioRepository
                .findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + funcionarioId));
        boolean elegivel = funcionarioCondominioRepository.findByFuncionarioId(funcionario.getId()).stream()
                .anyMatch(v -> v.getPerfil() != null
                        && v.getSituacao() == Situacao.ativo
                        && v.getFuncionario().getSituacao() == Situacao.ativo);
        if (!elegivel) {
            throw new InvalidRequestException(
                    "O funcionário " + funcionarioId + " não tem login ativo nesse condomínio");
        }
        return funcionario;
    }

    private boolean ehFuncionario(ContextoAutenticado contexto) {
        return TipoPessoa.funcionario.name().equals(contexto.tipoPapel());
    }

    private ConversaPrivada buscarConversa(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada: " + id));
    }

    private Morador buscarMorador(Integer id) {
        return moradorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id));
    }

    private Funcionario buscarFuncionario(Integer id) {
        return funcionarioRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }

    private Map<Integer, List<MensagemPrivadaDocumentoResponse>> agruparAnexosPorMensagem(List<Integer> mensagemIds) {
        if (mensagemIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<MensagemPrivadaDocumentoResponse>> mapa = new HashMap<>();
        for (MensagemPrivadaDocumento documento : documentoRepository.findByMensagemIdIn(mensagemIds)) {
            mapa.computeIfAbsent(documento.getMensagem().getId(), k -> new ArrayList<>())
                    .add(MensagemPrivadaDocumentoResponse.from(documento, caminhoArquivo(documento)));
        }
        return mapa;
    }

    private String caminhoArquivo(MensagemPrivadaDocumento documento) {
        return "/api/mensagem-privada-documentos/%d/arquivo".formatted(documento.getId());
    }

    private Map<Integer, List<MensagemPrivada>> agruparPorConversa(List<MensagemPrivada> mensagens) {
        Map<Integer, List<MensagemPrivada>> mapa = new HashMap<>();
        for (MensagemPrivada mensagem : mensagens) {
            mapa.computeIfAbsent(mensagem.getConversa().getId(), k -> new ArrayList<>()).add(mensagem);
        }
        return mapa;
    }

    private Map<Integer, List<ConversaPrivadaDestinatario>> agruparDestinatariosPorConversa(
            List<ConversaPrivadaDestinatario> destinatarios) {
        Map<Integer, List<ConversaPrivadaDestinatario>> mapa = new HashMap<>();
        for (ConversaPrivadaDestinatario destinatario : destinatarios) {
            mapa.computeIfAbsent(destinatario.getConversa().getId(), k -> new ArrayList<>()).add(destinatario);
        }
        return mapa;
    }
}
