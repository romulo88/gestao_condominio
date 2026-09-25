package com.condominiogestao.demanda;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.demanda.dto.DemandaAprovarRequest;
import com.condominiogestao.demanda.dto.DemandaCreateRequest;
import com.condominiogestao.demanda.dto.DemandaMoverKanbanRequest;
import com.condominiogestao.demanda.dto.DemandaMudancaStatusResponse;
import com.condominiogestao.demanda.dto.DemandaPaginaResponse;
import com.condominiogestao.demanda.dto.DemandaReprovarRequest;
import com.condominiogestao.demanda.dto.DemandaResponse;
import com.condominiogestao.demanda.dto.ResponsavelResumoResponse;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.kanban.StatusKanban;
import com.condominiogestao.kanban.StatusKanbanRepository;
import com.condominiogestao.morador.Morador;
import com.condominiogestao.morador.MoradorRepository;
import com.condominiogestao.notificacao.EmailService;
import com.condominiogestao.notificacao.EmailTemplates;
import com.condominiogestao.pessoa.PessoaFotoService;
import com.condominiogestao.security.ContextoAutenticado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sempre opera no condomínio do PRÓPRIO contexto - quem cadastra uma demanda é sempre
 * morador ou funcionário logado num condomínio (administrador não cadastra demanda, não
 * tem condomínio próprio - mesmo padrão do quadro de avisos antes do administrador
 * existir).
 *
 * <p>Sigilo (item 4.8): {@link #listar} já filtra quem enxerga uma demanda sigilosa -
 * síndico/sub-síndico do condomínio, o solicitante, quem marcou como sigilosa
 * ({@link Demanda#getFuncionarioMarcouSigilo()}), e quem estiver em
 * {@link DemandaAcessoSigiloso} (ver {@link DemandaAcessoSigilosoService}, que também
 * gerencia quem mais pode ver). ⚠️ Isso só filtra a LISTAGEM - as ações
 * (aprovar/reprovar/mover no Kanban/criar etapa/anexar etiqueta) continuam checando só
 * "é funcionário deste condomínio", sem saber se a demanda é sigilosa - alguém que já
 * soubesse o id de uma demanda sigilosa que não devia ver ainda conseguiria agir nela
 * diretamente pela API. Endurecer isso é um próximo passo.
 */
@Service
@Transactional(readOnly = true)
public class DemandaService {

    private final DemandaRepository repository;
    private final CondominioRepository condominioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MoradorRepository moradorRepository;
    private final StatusKanbanRepository statusKanbanRepository;
    private final DemandaStatusKanbanHistoricoRepository historicoRepository;
    private final DemandaEtiquetaRepository demandaEtiquetaRepository;
    private final DemandaAcessoSigilosoRepository acessoSigilosoRepository;
    private final DemandaDocumentoRepository documentoRepository;
    private final DemandaResponsavelRepository responsavelRepository;
    private final DemandaNotaRepository notaRepository;
    private final DemandaAcompanhamentoRepository acompanhamentoRepository;
    private final DemandaEtapaRepository etapaRepository;
    private final PessoaFotoService pessoaFotoService;
    private final EmailService emailService;

    public DemandaService(
            DemandaRepository repository,
            CondominioRepository condominioRepository,
            FuncionarioRepository funcionarioRepository,
            MoradorRepository moradorRepository,
            StatusKanbanRepository statusKanbanRepository,
            DemandaStatusKanbanHistoricoRepository historicoRepository,
            DemandaEtiquetaRepository demandaEtiquetaRepository,
            DemandaAcessoSigilosoRepository acessoSigilosoRepository,
            DemandaDocumentoRepository documentoRepository,
            DemandaResponsavelRepository responsavelRepository,
            DemandaNotaRepository notaRepository,
            DemandaAcompanhamentoRepository acompanhamentoRepository,
            DemandaEtapaRepository etapaRepository,
            PessoaFotoService pessoaFotoService,
            EmailService emailService) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.moradorRepository = moradorRepository;
        this.statusKanbanRepository = statusKanbanRepository;
        this.historicoRepository = historicoRepository;
        this.demandaEtiquetaRepository = demandaEtiquetaRepository;
        this.acessoSigilosoRepository = acessoSigilosoRepository;
        this.documentoRepository = documentoRepository;
        this.responsavelRepository = responsavelRepository;
        this.notaRepository = notaRepository;
        this.acompanhamentoRepository = acompanhamentoRepository;
        this.etapaRepository = etapaRepository;
        this.pessoaFotoService = pessoaFotoService;
        this.emailService = emailService;
    }

    /** Pedido do Romulo: avisar por e-mail quando a demanda é aprovada/reprovada - só
     * quando o SOLICITANTE é morador (funcionário não recebe, ele mesmo é quem decide).
     * Chamada depois de {@code repository.save} em {@link #aprovar}/{@link #reprovar}, nos
     * TRÊS desfechos possíveis - {@code raiaNome} e {@code respostaFuncionario} são
     * mutuamente exclusivos (cada chamador passa só um dos dois, o outro null): aprovada
     * COM coluna manda a raia; aprovada SEM coluna ("de imediato") e reprovada mandam a
     * justificativa do funcionário (ver {@link EmailTemplates#demandaDecisao}). Best-effort
     * - ver {@link EmailService#enviar}. */
    private void notificarDecisaoDemanda(Demanda demanda, String raiaNome, String respostaFuncionario) {
        Morador solicitante = demanda.getMoradorSolicitante();
        if (solicitante == null || solicitante.getEmail() == null || solicitante.getEmail().isBlank()) {
            return;
        }
        boolean aprovada = demanda.getStatusAprovacao() == DemandaStatusAprovacao.aprovada;
        String assunto = "Demanda #" + demanda.getId() + " - " + (aprovada ? "Aprovada" : "Reprovada");
        EmailTemplates.CorpoEmail corpo = EmailTemplates.demandaDecisao(
                demanda.getId(),
                demanda.getTitulo(),
                demanda.getDescricao(),
                aprovada,
                demanda.getCondominio().getNome(),
                raiaNome,
                respostaFuncionario);
        emailService.enviar(solicitante.getEmail(), assunto, corpo);
    }

    /** Pedido do Romulo: aviso ADICIONAL por e-mail quando a demanda entra numa coluna
     * finalística ({@code StatusKanban.finalistico}) - além do (nunca no lugar do) e-mail
     * de aprovação/reprovação. Chamada em {@link #aprovar} (coluna escolhida já nasce
     * finalística) e em {@link #moverKanban} (card arrastado pra uma). Mesma checagem de
     * "só morador com e-mail" de {@link #notificarDecisaoDemanda} - best-effort. */
    private void notificarFinalizacao(Demanda demanda) {
        Morador solicitante = demanda.getMoradorSolicitante();
        if (solicitante == null || solicitante.getEmail() == null || solicitante.getEmail().isBlank()) {
            return;
        }
        EmailTemplates.CorpoEmail corpo = EmailTemplates.demandaFinalizada(
                demanda.getId(),
                demanda.getTitulo(),
                demanda.getDescricao(),
                demanda.getCondominio().getNome(),
                demanda.getStatusKanban().getNome());
        emailService.enviar(solicitante.getEmail(), "Demanda #" + demanda.getId() + " - Finalizada", corpo);
    }

    /** Sobrecarga sem {@code todas} - comportamento de sempre (`/demandas`, acompanhamento
     * pessoal do morador). */
    public List<DemandaResponse> listar(ContextoAutenticado contexto) {
        return listar(contexto, false);
    }

    /** Funcionário sempre vê todas as demandas do condomínio (com etiquetas) que ele tem
     * permissão de ver (ver sigilo acima). Morador vê só as que ele mesmo abriu por
     * padrão ({@code todas=false}, usado por `/demandas` - acompanhamento pessoal); com
     * {@code todas=true} (usado pelo quadro Kanban - visão geral do condomínio) morador
     * também vê todas, igual funcionário - a demanda sigilosa continua protegida do mesmo
     * jeito nos dois casos, então isso não é uma brecha de privacidade, só uma tela a mais
     * que dá visibilidade ampla (o morador já TEM direito de ver essas demandas, só não
     * tinha pedido antes de existir o quadro). Morador só vê a etiqueta marcada como
     * {@code visivelMorador} (pedido do Romulo) - funcionário sempre vê todas, mesmo com
     * {@code todas=true} (ver {@link EtiquetaService}). Sempre em ordem decrescente de
     * criação (mais nova primeiro). */
    public List<DemandaResponse> listar(ContextoAutenticado contexto, boolean todas) {
        exigirFuncionarioOuMorador(contexto);
        boolean ehFuncionario = TipoPessoa.funcionario.name().equals(contexto.tipoPapel());
        List<Demanda> carregadas = (ehFuncionario || todas)
                ? repository.findByCondominioId(contexto.condominioId())
                : repository.findByCondominioIdAndMoradorSolicitanteId(contexto.condominioId(), contexto.pessoaId());

        List<Integer> idsSigilosos = carregadas.stream().filter(Demanda::isSigilosa).map(Demanda::getId).toList();
        Map<Integer, List<DemandaAcessoSigiloso>> acessosPorDemanda = idsSigilosos.isEmpty()
                ? Map.of()
                : acessoSigilosoRepository.findByDemandaIdIn(idsSigilosos).stream()
                        .collect(Collectors.groupingBy(a -> a.getDemanda().getId()));

        // Coluna pode ficar oculta pro morador (pedido do Romulo) - a coluna em si já some
        // do `StatusKanbanService.listarPorCondominio`, mas filtrar aqui também evita que a
        // demanda vaze pela API mesmo assim (defesa em profundidade, mesmo espírito do
        // filtro de sigilo acima). Só vale pro quadro Kanban (`todas=true`) - a lista
        // pessoal (`/demandas`, `todas=false`) sempre mostrou as próprias demandas do
        // morador, coluna oculta ou não, e mudar isso não foi pedido.
        List<Demanda> demandas = carregadas.stream()
                .filter(d -> podeVerSigilosa(contexto, d, acessosPorDemanda))
                .filter(d -> ehFuncionario || !todas || colunaVisivelExternamente(d))
                .sorted(Comparator.comparing(Demanda::getCreatedAt).reversed())
                .toList();

        List<Integer> idsDemandas = demandas.stream().map(Demanda::getId).toList();
        Set<Integer> idsComAnexo = buscarIdsComAnexo(idsDemandas);
        Set<Integer> idsComNotaPendente = buscarIdsComNotaPendente(idsDemandas);
        // Contorno vermelho/verde de etapa vencida/vigente no card do Kanban (pedido do
        // Romulo: "para morador, colocar o mesmo padrão visual de contornos de etapas no
        // kanban") - desde a v134 o morador também VÊ a lista de etapas (só leitura), então
        // esse indicador deixou de ser exclusivo de funcionário; calculado uma vez só,
        // reaproveitado nos dois branches abaixo.
        Map<Integer, EtapaFlags> flagsEtapasPorDemanda = buscarFlagsEtapasPorDemanda(idsDemandas);
        // Calculado uma vez só, reaproveitado nos dois branches abaixo (mesmo espírito das
        // etapas na v136) - o morador só recebe a fatia filtrada por `visivelMorador` logo
        // em seguida, funcionário recebe a lista inteira sem filtro nenhum.
        Map<Integer, List<EtiquetaResponse>> etiquetasPorDemanda = buscarEtiquetasPorDemanda(idsDemandas);
        // KPI de "dias parado" do Kanban (pedido do Romulo) - desde quando cada demanda está
        // na coluna atual, em lote. Calculado sempre (custa uma query já com os ids em mãos)
        // mesmo na listagem pessoal (`todas=false`) - é a mesma informação, sem restrição de
        // visibilidade adicional (`statusKanbanNome` já é exposto pros dois papéis).
        Map<Integer, LocalDateTime> statusKanbanDesdePorDemanda = buscarStatusKanbanDesdePorDemanda(idsDemandas);

        if (!ehFuncionario) {
            // "Acompanhar" (v116) é morador-only - só calcula em lote aqui, funcionário
            // nunca acompanha (sempre false pra ele, ver branch de baixo).
            Set<Integer> idsAcompanhados = buscarIdsAcompanhados(contexto.pessoaId(), idsDemandas);
            // Responsável é informação interna (pedido do Romulo: "essa informação é
            // apenas para funcionários") - morador nunca recebe, nem no quadro Kanban.
            return demandas.stream()
                    .map(d -> DemandaResponse.from(
                            d,
                            etiquetasPorDemanda.getOrDefault(d.getId(), List.of()).stream()
                                    .filter(EtiquetaResponse::visivelMorador)
                                    .toList(),
                            podeGerenciarSigilo(contexto, d),
                            idsComAnexo.contains(d.getId()),
                            idsComNotaPendente.contains(d.getId()),
                            flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vencida(),
                            flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vigente(),
                            List.of(),
                            podeAcompanhar(contexto, d),
                            idsAcompanhados.contains(d.getId()),
                            statusKanbanDesdePorDemanda.get(d.getId())))
                    .toList();
        }

        Map<Integer, List<ResponsavelResumoResponse>> responsaveisPorDemanda = buscarResponsaveisPorDemanda(idsDemandas);
        return demandas.stream()
                .map(d -> DemandaResponse.from(
                        d,
                        etiquetasPorDemanda.getOrDefault(d.getId(), List.of()),
                        podeGerenciarSigilo(contexto, d),
                        idsComAnexo.contains(d.getId()),
                        idsComNotaPendente.contains(d.getId()),
                        flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vencida(),
                        flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vigente(),
                        responsaveisPorDemanda.getOrDefault(d.getId(), List.of()),
                        false,
                        false,
                        statusKanbanDesdePorDemanda.get(d.getId())))
                .toList();
    }

    /** Página da listagem de `/demandas` (pedido do Romulo: "paginar a listagem das
     * demandas em 20 registros") - mesma visibilidade de {@code listar(contexto, false)}
     * (funcionário vê todas do condomínio, morador só as próprias; sigilo filtrado do
     * mesmo jeito), só que devolvendo uma página por vez em vez da lista inteira. Os
     * filtros de `/demandas` (busca por descrição, status de aprovação OU coluna do
     * Kanban - convenção `"kanban:<id>"`, nota não lida, etapa vencida) que antes eram só
     * client-side (sobre a lista inteira já carregada) viraram parâmetros aqui, porque
     * paginar e filtrar client-side ao mesmo tempo não faz sentido - filtrar só dentro da
     * página exibida esconderia resultados que estariam em outra página. Mesma regra de
     * "nota não lida/etapa vencida ignoram o status" que a tela já tinha (ver comentário
     * em `demandas/page.tsx`), replicada aqui pra não mudar o comportamento.
     *
     * <p>O sigilo (e as duas flags de nota/etapa, usadas tanto pra filtrar quanto pra
     * exibir) continuam calculados pra TODAS as demandas visíveis do condomínio antes de
     * paginar - não tem como saber se uma demanda bate o filtro sem isso, então essa parte
     * não fica mais barata só por causa da paginação (era assim antes também). O que fica
     * mais barato de verdade é o resto do enriquecimento (etiquetas, anexos, responsáveis)
     * - agora calculado só pras 20 demandas da página, não pra todas as do condomínio. */
    public DemandaPaginaResponse listarPagina(
            ContextoAutenticado contexto,
            String busca,
            String status,
            boolean notaNaoLida,
            boolean etapaVencida,
            int pagina,
            int tamanho) {
        exigirFuncionarioOuMorador(contexto);
        boolean ehFuncionario = TipoPessoa.funcionario.name().equals(contexto.tipoPapel());
        List<Demanda> carregadas = ehFuncionario
                ? repository.findByCondominioId(contexto.condominioId())
                : repository.findByCondominioIdAndMoradorSolicitanteId(contexto.condominioId(), contexto.pessoaId());

        List<Integer> idsSigilosos = carregadas.stream().filter(Demanda::isSigilosa).map(Demanda::getId).toList();
        Map<Integer, List<DemandaAcessoSigiloso>> acessosPorDemanda = idsSigilosos.isEmpty()
                ? Map.of()
                : acessoSigilosoRepository.findByDemandaIdIn(idsSigilosos).stream()
                        .collect(Collectors.groupingBy(a -> a.getDemanda().getId()));

        List<Demanda> visiveis = carregadas.stream()
                .filter(d -> podeVerSigilosa(contexto, d, acessosPorDemanda))
                .sorted(Comparator.comparing(Demanda::getCreatedAt).reversed())
                .toList();

        List<Integer> idsVisiveis = visiveis.stream().map(Demanda::getId).toList();
        Set<Integer> idsComNotaPendente = buscarIdsComNotaPendente(idsVisiveis);
        Map<Integer, EtapaFlags> flagsEtapasPorDemanda = buscarFlagsEtapasPorDemanda(idsVisiveis);

        boolean statusInformado = status != null && !status.isBlank();
        Integer colunaKanbanId = (statusInformado && status.startsWith("kanban:"))
                ? Integer.valueOf(status.substring("kanban:".length()))
                : null;
        String buscaNormalizada = busca == null ? "" : busca.trim().toLowerCase();

        List<Demanda> filtradas = visiveis.stream()
                .filter(d -> buscaNormalizada.isEmpty() || d.getDescricao().toLowerCase().contains(buscaNormalizada))
                .filter(d -> {
                    boolean bateStatus = notaNaoLida
                            || etapaVencida
                            || !statusInformado
                            || (colunaKanbanId != null
                                    ? d.getStatusKanban() != null && colunaKanbanId.equals(d.getStatusKanban().getId())
                                    : d.getStatusAprovacao().name().equals(status));
                    boolean bateNotaNaoLida = !notaNaoLida || idsComNotaPendente.contains(d.getId());
                    boolean bateEtapaVencida = !etapaVencida
                            || flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vencida();
                    return bateStatus && bateNotaNaoLida && bateEtapaVencida;
                })
                .toList();

        int totalItens = filtradas.size();
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), 100);
        int totalPaginas = (int) Math.ceil(totalItens / (double) tamanhoSeguro);
        int paginaSegura = Math.max(pagina, 0);
        int inicio = Math.min(paginaSegura * tamanhoSeguro, totalItens);
        int fim = Math.min(inicio + tamanhoSeguro, totalItens);
        List<Demanda> demandasDaPagina = filtradas.subList(inicio, fim);

        List<Integer> idsPagina = demandasDaPagina.stream().map(Demanda::getId).toList();
        Set<Integer> idsComAnexo = buscarIdsComAnexo(idsPagina);
        Map<Integer, List<EtiquetaResponse>> etiquetasPorDemanda = buscarEtiquetasPorDemanda(idsPagina);

        List<DemandaResponse> itens;
        if (!ehFuncionario) {
            Set<Integer> idsAcompanhados = buscarIdsAcompanhados(contexto.pessoaId(), idsPagina);
            itens = demandasDaPagina.stream()
                    .map(d -> DemandaResponse.from(
                            d,
                            etiquetasPorDemanda.getOrDefault(d.getId(), List.of()).stream()
                                    .filter(EtiquetaResponse::visivelMorador)
                                    .toList(),
                            podeGerenciarSigilo(contexto, d),
                            idsComAnexo.contains(d.getId()),
                            idsComNotaPendente.contains(d.getId()),
                            flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vencida(),
                            flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vigente(),
                            List.of(),
                            podeAcompanhar(contexto, d),
                            idsAcompanhados.contains(d.getId())))
                    .toList();
        } else {
            Map<Integer, List<ResponsavelResumoResponse>> responsaveisPorDemanda =
                    buscarResponsaveisPorDemanda(idsPagina);
            itens = demandasDaPagina.stream()
                    .map(d -> DemandaResponse.from(
                            d,
                            etiquetasPorDemanda.getOrDefault(d.getId(), List.of()),
                            podeGerenciarSigilo(contexto, d),
                            idsComAnexo.contains(d.getId()),
                            idsComNotaPendente.contains(d.getId()),
                            flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vencida(),
                            flagsEtapasPorDemanda.getOrDefault(d.getId(), EtapaFlags.NENHUMA).vigente(),
                            responsaveisPorDemanda.getOrDefault(d.getId(), List.of()),
                            false,
                            false))
                    .toList();
        }

        boolean existeNotaNaoLida = !idsComNotaPendente.isEmpty();
        boolean existeEtapaVencida = flagsEtapasPorDemanda.values().stream().anyMatch(EtapaFlags::vencida);
        return new DemandaPaginaResponse(
                itens, paginaSegura, totalPaginas, totalItens, existeNotaNaoLida, existeEtapaVencida);
    }

    @Transactional
    public DemandaResponse criar(ContextoAutenticado contexto, DemandaCreateRequest request) {
        exigirFuncionarioOuMorador(contexto);

        Condominio condominio = condominioRepository
                .findById(contexto.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + contexto.condominioId()));

        Demanda demanda = new Demanda();
        demanda.setCondominio(condominio);
        demanda.setTitulo(request.titulo());
        demanda.setDescricao(request.descricao());
        demanda.setSigilosa(request.sigilosa());
        demanda.setIdentificarSolicitante(request.identificarSolicitante());

        if (TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            Morador morador = moradorRepository
                    .findById(contexto.pessoaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + contexto.pessoaId()));
            demanda.setMoradorSolicitante(morador);
        } else {
            Funcionario funcionario = buscarFuncionario(contexto.pessoaId());
            demanda.setFuncionarioSolicitante(funcionario);
            // Quem abriu já marcando sigilosa é, por definição, quem "marcou" - ganha o
            // mesmo poder de indicar mais gente que um funcionário marcaria depois via
            // alternarSigilo (ver Demanda.funcionarioMarcouSigilo).
            if (request.sigilosa()) {
                demanda.setFuncionarioMarcouSigilo(funcionario);
            }
        }
        // statusAprovacao já nasce 'pendente' por default no campo da entidade

        Demanda salva = repository.save(demanda);
        return DemandaResponse.from(salva, podeGerenciarSigilo(contexto, salva));
    }

    /**
     * Aprova a demanda - de dois jeitos possíveis:
     * <ul>
     *   <li>Com {@code statusKanbanId}: manda pra essa coluna do Kanban do próprio
     *   condomínio (normalmente a primeira, tipo "Fila") e já gera a primeira linha do
     *   histórico de transição ({@link DemandaStatusKanbanHistorico}).</li>
     *   <li>Sem {@code statusKanbanId} (pedido do Romulo): aprova sem entrar no Kanban -
     *   exige {@code justificativa} em vez disso (mesmo espírito de {@link #reprovar}).
     *   Fica sem coluna até algum dia ser movida pra uma via {@link #moverKanban}.</li>
     * </ul>
     * Exatamente um dos dois é esperado - nenhuma anotação de bean validation expressa
     * "um OU outro", por isso a checagem é aqui.
     */
    @Transactional
    public DemandaResponse aprovar(ContextoAutenticado contexto, Integer id, DemandaAprovarRequest request) {
        Demanda demanda = buscarDemanda(id);
        exigirFuncionarioDoCondominio(contexto, demanda);
        exigirPendente(demanda);

        Funcionario funcionario = buscarFuncionario(contexto.pessoaId());
        demanda.setStatusAprovacao(DemandaStatusAprovacao.aprovada);
        demanda.setFuncionarioAprovador(funcionario);
        demanda.setDataAprovacao(LocalDateTime.now());

        if (request.statusKanbanId() == null) {
            if (request.justificativa() == null || request.justificativa().isBlank()) {
                throw new InvalidRequestException(
                        "Informe uma justificativa pra aprovar sem Kanban, ou escolha uma coluna");
            }
            demanda.setJustificativaAprovacao(request.justificativa());
            Demanda salva = repository.save(demanda);
            notificarDecisaoDemanda(salva, null, salva.getJustificativaAprovacao());
            EtapaFlags flagsEtapas = flagsEtapas(salva.getId());
            return DemandaResponse.from(
                    salva,
                    buscarEtiquetas(salva.getId()),
                    podeGerenciarSigilo(contexto, salva),
                    documentoRepository.existsByDemandaId(salva.getId()),
                    temNotaPendente(salva.getId()),
                    flagsEtapas.vencida(),
                    flagsEtapas.vigente(),
                    buscarResponsaveis(salva.getId()),
                    false,
                    false);
        }

        StatusKanban statusKanban = statusKanbanRepository
                .findById(request.statusKanbanId())
                .orElseThrow(() -> new ResourceNotFoundException("Coluna de Kanban não encontrada: " + request.statusKanbanId()));
        if (!statusKanban.getCondominio().getId().equals(demanda.getCondominio().getId())) {
            throw new InvalidRequestException("Essa coluna de Kanban não pertence a este condomínio");
        }

        demanda.setStatusKanban(statusKanban);
        // Primeira entrada na coluna sempre no fim da lista (pedido do Romulo: mesmo
        // comportamento de sempre, só que com uma ordem de verdade em vez de depender de
        // `createdAt`) - ver `DemandaService.moverKanban` pra reordenação de verdade.
        demanda.setOrdem(repository.findByStatusKanbanIdOrderByOrdemAsc(statusKanban.getId()).size());
        Demanda salva = repository.save(demanda);

        DemandaStatusKanbanHistorico historico = new DemandaStatusKanbanHistorico();
        historico.setDemanda(salva);
        historico.setStatusNovo(statusKanban);
        historico.setFuncionario(funcionario);
        historicoRepository.save(historico);
        notificarDecisaoDemanda(salva, statusKanban.getNome(), null);
        if (statusKanban.isFinalistico()) {
            notificarFinalizacao(salva);
        }

        EtapaFlags flagsEtapas = flagsEtapas(salva.getId());
        return DemandaResponse.from(
                salva,
                buscarEtiquetas(salva.getId()),
                podeGerenciarSigilo(contexto, salva),
                documentoRepository.existsByDemandaId(salva.getId()),
                temNotaPendente(salva.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                buscarResponsaveis(salva.getId()),
                false,
                false);
    }

    /**
     * Reprova sem passar pelo Kanban - usada quando a demanda não vai precisar de
     * nenhuma etapa (ex: já existe outra igual aberta, ou foi resolvida na hora).
     */
    @Transactional
    public DemandaResponse reprovar(ContextoAutenticado contexto, Integer id, DemandaReprovarRequest request) {
        Demanda demanda = buscarDemanda(id);
        exigirFuncionarioDoCondominio(contexto, demanda);
        exigirPendente(demanda);

        demanda.setStatusAprovacao(DemandaStatusAprovacao.reprovada);
        demanda.setJustificativaReprovacao(request.justificativa());
        demanda.setFuncionarioAprovador(buscarFuncionario(contexto.pessoaId()));
        demanda.setDataAprovacao(LocalDateTime.now());

        Demanda salva = repository.save(demanda);
        notificarDecisaoDemanda(salva, null, salva.getJustificativaReprovacao());
        EtapaFlags flagsEtapas = flagsEtapas(salva.getId());
        return DemandaResponse.from(
                salva,
                buscarEtiquetas(salva.getId()),
                podeGerenciarSigilo(contexto, salva),
                documentoRepository.existsByDemandaId(salva.getId()),
                temNotaPendente(salva.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                buscarResponsaveis(salva.getId()),
                false,
                false);
    }

    /**
     * Move o card no quadro - arrastar de uma coluna pra outra, E/OU reordenar dentro da
     * MESMA coluna (pedido do Romulo: agrupar cards de assuntos parecidos lado a lado).
     * Só demanda já aprovada (já tem uma coluna) pode mudar de coluna; a entrada na
     * primeira coluna é sempre via {@link #aprovar}. {@code antesDaDemandaId} (ver {@link
     * DemandaMoverKanbanRequest}) decide a posição dentro da coluna de destino - {@code
     * null} vai pro fim, preenchido insere a demanda arrastada imediatamente antes da
     * demanda referenciada. Só gera histórico de transição/notificação de finalização
     * quando a coluna muda de verdade - reordenar dentro da mesma coluna não é uma
     * transição de status.
     */
    @Transactional
    public DemandaResponse moverKanban(ContextoAutenticado contexto, Integer id, DemandaMoverKanbanRequest request) {
        Demanda demanda = buscarDemanda(id);
        exigirFuncionarioDoCondominio(contexto, demanda);

        if (demanda.getStatusAprovacao() != DemandaStatusAprovacao.aprovada) {
            throw new ConflictException("Só demanda aprovada tem coluna no Kanban pra mudar");
        }

        StatusKanban novaColuna = statusKanbanRepository
                .findById(request.statusKanbanId())
                .orElseThrow(() -> new ResourceNotFoundException("Coluna de Kanban não encontrada: " + request.statusKanbanId()));
        if (!novaColuna.getCondominio().getId().equals(demanda.getCondominio().getId())) {
            throw new InvalidRequestException("Essa coluna de Kanban não pertence a este condomínio");
        }

        StatusKanban colunaAnterior = demanda.getStatusKanban();
        boolean mudouColuna = colunaAnterior == null || !colunaAnterior.getId().equals(novaColuna.getId());

        // Renumera a coluna de destino inteira com a demanda na posição pedida - simples
        // e robusto pro volume esperado (poucas dezenas de cards por coluna), evita ordem
        // fracionária. Remove a própria demanda da lista antes de reinserir, senão
        // duplicaria quando a reordenação for dentro da MESMA coluna.
        List<Demanda> demandasColuna = repository.findByStatusKanbanIdOrderByOrdemAsc(novaColuna.getId()).stream()
                .filter(d -> !d.getId().equals(demanda.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        int indiceDestino;
        if (request.antesDaDemandaId() != null) {
            indiceDestino = -1;
            for (int i = 0; i < demandasColuna.size(); i++) {
                if (demandasColuna.get(i).getId().equals(request.antesDaDemandaId())) {
                    indiceDestino = i;
                    break;
                }
            }
            if (indiceDestino < 0) {
                throw new InvalidRequestException("A demanda de referência não está nessa coluna");
            }
        } else {
            indiceDestino = demandasColuna.size();
        }
        demandasColuna.add(indiceDestino, demanda);
        for (int i = 0; i < demandasColuna.size(); i++) {
            demandasColuna.get(i).setOrdem(i);
        }
        demanda.setStatusKanban(novaColuna);
        repository.saveAll(demandasColuna);

        // Trocou de coluna? A coluna de ORIGEM fica com um buraco na sequência (a demanda
        // que saiu levava um número do meio) - renumera ela também, senão uma inserção
        // futura no fim (`indiceDestino = size()`) pode colidir com uma ordem que já existe
        // ali (duas demandas com a mesma `ordem`, ordem relativa entre elas indefinida).
        if (mudouColuna && colunaAnterior != null) {
            List<Demanda> demandasColunaAnterior = repository.findByStatusKanbanIdOrderByOrdemAsc(colunaAnterior.getId());
            for (int i = 0; i < demandasColunaAnterior.size(); i++) {
                demandasColunaAnterior.get(i).setOrdem(i);
            }
            repository.saveAll(demandasColunaAnterior);
        }

        Demanda salva = demanda;

        if (mudouColuna) {
            Funcionario funcionario = buscarFuncionario(contexto.pessoaId());
            DemandaStatusKanbanHistorico historico = new DemandaStatusKanbanHistorico();
            historico.setDemanda(salva);
            historico.setStatusAnterior(colunaAnterior);
            historico.setStatusNovo(novaColuna);
            historico.setFuncionario(funcionario);
            historicoRepository.save(historico);
            if (novaColuna.isFinalistico()) {
                notificarFinalizacao(salva);
            }
        }

        EtapaFlags flagsEtapas = flagsEtapas(salva.getId());
        return DemandaResponse.from(
                salva,
                buscarEtiquetas(salva.getId()),
                podeGerenciarSigilo(contexto, salva),
                documentoRepository.existsByDemandaId(salva.getId()),
                temNotaPendente(salva.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                buscarResponsaveis(salva.getId()),
                false,
                false);
    }

    /**
     * Único jeito de funcionário "editar" uma demanda depois de criada - só alterna
     * sigilosa/não-sigilosa, nada mais (título, descrição etc. continuam fixos desde a
     * criação). Funciona em qualquer status (pendente/aprovada/reprovada). Quem alterna
     * pra sigilosa vira {@code funcionarioMarcouSigilo} (ganha acesso e pode indicar mais
     * gente); voltar pra não-sigilosa limpa esse campo.
     */
    @Transactional
    public DemandaResponse alternarSigilo(ContextoAutenticado contexto, Integer id) {
        Demanda demanda = buscarDemanda(id);
        exigirFuncionarioDoCondominio(contexto, demanda);

        boolean novoValor = !demanda.isSigilosa();
        demanda.setSigilosa(novoValor);
        demanda.setFuncionarioMarcouSigilo(novoValor ? buscarFuncionario(contexto.pessoaId()) : null);
        Demanda salva = repository.save(demanda);

        EtapaFlags flagsEtapas = flagsEtapas(salva.getId());
        return DemandaResponse.from(
                salva,
                buscarEtiquetas(salva.getId()),
                podeGerenciarSigilo(contexto, salva),
                documentoRepository.existsByDemandaId(salva.getId()),
                temNotaPendente(salva.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                buscarResponsaveis(salva.getId()),
                false,
                false);
    }

    /**
     * Arquiva a demanda (pedido do Romulo) - só faz sentido quando ela já está numa coluna
     * finalística ({@link StatusKanban#isFinalistico()}), ou seja, o fluxo dela acabou.
     * Não apaga nada: só marca {@code arquivada = true} pra o card sair do estado ativo.
     * Qualquer funcionário do condomínio (mesmo critério de {@link #moverKanban} e das
     * outras ações de card).
     */
    @Transactional
    public DemandaResponse arquivar(ContextoAutenticado contexto, Integer id) {
        Demanda demanda = buscarDemanda(id);
        exigirFuncionarioDoCondominio(contexto, demanda);

        StatusKanban coluna = demanda.getStatusKanban();
        if (coluna == null || !coluna.isFinalistico()) {
            throw new ConflictException("Só dá pra arquivar demanda que está numa coluna finalística do Kanban");
        }

        demanda.setArquivada(true);
        Demanda salva = repository.save(demanda);

        EtapaFlags flagsEtapas = flagsEtapas(salva.getId());
        return DemandaResponse.from(
                salva,
                buscarEtiquetas(salva.getId()),
                podeGerenciarSigilo(contexto, salva),
                documentoRepository.existsByDemandaId(salva.getId()),
                temNotaPendente(salva.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                buscarResponsaveis(salva.getId()),
                false,
                false);
    }

    /**
     * Alerta de login do morador (pedido do Romulo): tudo que mudou de status nas
     * demandas QUE ELE MESMO abriu, desde uma data - dois tipos de evento, juntados numa
     * lista só, ordenada por quando aconteceu:
     * <ul>
     *   <li>aprovação/reprovação - vem direto de {@link Demanda} (transição única,
     *       {@code dataAprovacao} grava os dois casos, ver {@link #aprovar}/{@link #reprovar});</li>
     *   <li>movimentação de coluna no Kanban - vem de {@link DemandaStatusKanbanHistorico},
     *       só as linhas com {@code statusAnterior} preenchido (a entrada inicial na
     *       primeira coluna acontece no mesmo instante da aprovação, então já está coberta
     *       pelo evento de aprovação - incluir ela também aqui duplicaria o mesmo momento
     *       como dois avisos diferentes).</li>
     * </ul>
     * Só morador - funcionário não tem esse alerta (já acompanha tudo pelo Kanban).
     */
    public List<DemandaMudancaStatusResponse> mudancasStatus(ContextoAutenticado contexto, LocalDateTime desde) {
        if (!TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            throw new ForbiddenException("Esse alerta é só pra morador");
        }

        List<DemandaMudancaStatusResponse> eventos = new ArrayList<>();

        // Próprias demandas.
        repository
                .findByMoradorSolicitanteIdAndDataAprovacaoAfter(contexto.pessoaId(), desde)
                .forEach(d -> eventos.add(eventoDecisao(d, "propria")));
        historicoRepository
                .findByDemandaMoradorSolicitanteIdAndCreatedAtAfterOrderByCreatedAt(contexto.pessoaId(), desde)
                .stream()
                .filter(h -> h.getStatusAnterior() != null)
                .forEach(h -> eventos.add(eventoKanban(h, "propria")));

        // Demandas que o morador ACOMPANHA (funcionalidade "Acompanhar", v116, pedido do
        // Romulo: "mudança de status se junte ao alerta das suas próprias demandas").
        List<Integer> idsAcompanhados = acompanhamentoRepository.findByMoradorId(contexto.pessoaId()).stream()
                .map(a -> a.getDemanda().getId())
                .toList();
        if (!idsAcompanhados.isEmpty()) {
            repository
                    .findByIdInAndDataAprovacaoAfter(idsAcompanhados, desde)
                    .forEach(d -> eventos.add(eventoDecisao(d, "acompanhada")));
            historicoRepository
                    .findByDemandaIdInAndCreatedAtAfterOrderByCreatedAt(idsAcompanhados, desde)
                    .stream()
                    .filter(h -> h.getStatusAnterior() != null)
                    .forEach(h -> eventos.add(eventoKanban(h, "acompanhada")));
        }

        eventos.sort(Comparator.comparing(DemandaMudancaStatusResponse::data));
        return eventos;
    }

    private DemandaMudancaStatusResponse eventoDecisao(Demanda d, String origem) {
        boolean aprovada = d.getStatusAprovacao() == DemandaStatusAprovacao.aprovada;
        return new DemandaMudancaStatusResponse(
                d.getId(),
                d.getTitulo(),
                aprovada ? "aprovada" : "reprovada",
                null,
                null,
                aprovada ? d.getJustificativaAprovacao() : d.getJustificativaReprovacao(),
                d.getDataAprovacao(),
                origem);
    }

    private DemandaMudancaStatusResponse eventoKanban(DemandaStatusKanbanHistorico h, String origem) {
        return new DemandaMudancaStatusResponse(
                h.getDemanda().getId(),
                h.getDemanda().getTitulo(),
                "kanban",
                h.getStatusAnterior().getNome(),
                h.getStatusNovo().getNome(),
                null,
                h.getCreatedAt(),
                origem);
    }

    /**
     * Funcionalidade "Acompanhar" (pedido do Romulo): morador marca acompanhar uma
     * demanda que ele não abriu, pra ela entrar no alerta de mudança de status junto
     * das próprias (ver {@link #mudancasStatus}). Idempotente - marcar de novo não
     * duplica (o unique constraint no banco também garante isso). Exige visibilidade
     * normal da demanda (mesma regra do quadro Kanban, {@link #podeVer}) - não dá pra
     * acompanhar o que não tem direito de ver.
     */
    @Transactional
    public DemandaResponse acompanhar(ContextoAutenticado contexto, Integer id) {
        exigirMorador(contexto);
        Demanda demanda = buscarDemanda(id);
        if (!podeVer(contexto, demanda)) {
            throw new ForbiddenException("Você não tem acesso a essa demanda");
        }
        if (demanda.getMoradorSolicitante() != null
                && demanda.getMoradorSolicitante().getId().equals(contexto.pessoaId())) {
            throw new ConflictException("Você já é quem abriu essa demanda - não tem o que acompanhar");
        }

        if (acompanhamentoRepository.findByDemandaIdAndMoradorId(id, contexto.pessoaId()).isEmpty()) {
            DemandaAcompanhamento acompanhamento = new DemandaAcompanhamento();
            acompanhamento.setDemanda(demanda);
            acompanhamento.setMorador(buscarMorador(contexto.pessoaId()));
            acompanhamentoRepository.save(acompanhamento);
        }

        EtapaFlags flagsEtapas = flagsEtapas(demanda.getId());
        return DemandaResponse.from(
                demanda,
                List.of(),
                podeGerenciarSigilo(contexto, demanda),
                documentoRepository.existsByDemandaId(demanda.getId()),
                temNotaPendente(demanda.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                List.of(),
                true,
                true);
    }

    /** Desmarca "Acompanhar" - idempotente, não achar a linha não é erro. */
    @Transactional
    public DemandaResponse deixarDeAcompanhar(ContextoAutenticado contexto, Integer id) {
        exigirMorador(contexto);
        Demanda demanda = buscarDemanda(id);
        acompanhamentoRepository.deleteByDemandaIdAndMoradorId(id, contexto.pessoaId());

        EtapaFlags flagsEtapas = flagsEtapas(demanda.getId());
        return DemandaResponse.from(
                demanda,
                List.of(),
                podeGerenciarSigilo(contexto, demanda),
                documentoRepository.existsByDemandaId(demanda.getId()),
                temNotaPendente(demanda.getId()),
                flagsEtapas.vencida(),
                flagsEtapas.vigente(),
                List.of(),
                true,
                false);
    }

    /** Item 4.8: só síndico/sub-síndico do condomínio, ou o funcionário que marcou
     * sigilosa=true mais recentemente, "gerenciam" o sigilo (podem indicar mais gente -
     * ver {@link DemandaAcessoSigilosoService}, que duplica essa mesma checagem). */
    private boolean podeGerenciarSigilo(ContextoAutenticado contexto, Demanda demanda) {
        if (Autorizacao.ehGestorDoCondominio(contexto, demanda.getCondominio().getId())) {
            return true;
        }
        return TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && demanda.getFuncionarioMarcouSigilo() != null
                && demanda.getFuncionarioMarcouSigilo().getId().equals(contexto.pessoaId());
    }

    /** Funcionalidade "Acompanhar" (v116): o viewer pode acompanhar quando é morador E
     * não é o solicitante dessa demanda (não tem o que acompanhar na própria) - ver
     * {@code DemandaResponse.podeAcompanhar}, que controla o check no card do Kanban. */
    private boolean podeAcompanhar(ContextoAutenticado contexto, Demanda demanda) {
        if (!TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            return false;
        }
        return demanda.getMoradorSolicitante() == null
                || !demanda.getMoradorSolicitante().getId().equals(contexto.pessoaId());
    }

    /** Item 4.8: se não é sigilosa, todo mundo que já tem direito de ver a lista (ver
     * {@link #listar}) vê ela normalmente. Se é sigilosa, só quem gerencia o sigilo
     * ({@link #podeGerenciarSigilo}), o próprio solicitante, ou quem estiver em
     * {@code acessosPorDemanda} pra essa demanda. */
    private boolean podeVerSigilosa(
            ContextoAutenticado contexto, Demanda demanda, Map<Integer, List<DemandaAcessoSigiloso>> acessosPorDemanda) {
        if (!demanda.isSigilosa()) {
            return true;
        }
        if (podeGerenciarSigilo(contexto, demanda)) {
            return true;
        }

        boolean ehSolicitante = TipoPessoa.morador.name().equals(contexto.tipoPapel())
                ? demanda.getMoradorSolicitante() != null
                        && demanda.getMoradorSolicitante().getId().equals(contexto.pessoaId())
                : demanda.getFuncionarioSolicitante() != null
                        && demanda.getFuncionarioSolicitante().getId().equals(contexto.pessoaId());
        if (ehSolicitante) {
            return true;
        }

        boolean ehMorador = TipoPessoa.morador.name().equals(contexto.tipoPapel());
        return acessosPorDemanda.getOrDefault(demanda.getId(), List.of()).stream()
                .anyMatch(a -> ehMorador
                        ? a.getMorador() != null && a.getMorador().getId().equals(contexto.pessoaId())
                        : a.getFuncionario() != null && a.getFuncionario().getId().equals(contexto.pessoaId()));
    }

    /** Mesma regra de visibilidade usada no quadro Kanban ({@code listar(contexto, true)})
     * pra checar UMA demanda específica - usado por serviços satélites (histórico de
     * Kanban, ver {@link DemandaStatusKanbanHistoricoService}) que precisam da mesma
     * autorização sem duplicar a lógica de sigilo. */
    public boolean podeVer(ContextoAutenticado contexto, Demanda demanda) {
        boolean mesmoCondominio = demanda.getCondominio().getId().equals(contexto.condominioId());
        boolean ehFuncionarioOuMorador = TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                || TipoPessoa.morador.name().equals(contexto.tipoPapel());
        if (!mesmoCondominio || !ehFuncionarioOuMorador) {
            return false;
        }
        if (!demanda.isSigilosa()) {
            return true;
        }
        Map<Integer, List<DemandaAcessoSigiloso>> acessos =
                Map.of(demanda.getId(), acessoSigilosoRepository.findByDemandaIdIn(List.of(demanda.getId())));
        return podeVerSigilosa(contexto, demanda, acessos);
    }

    /** Demanda ainda sem coluna (pendente de aprovação) não some por causa disso - só a
     * que já está numa coluna marcada oculta (ver {@code StatusKanban.visivelExternamente}). */
    private boolean colunaVisivelExternamente(Demanda demanda) {
        return demanda.getStatusKanban() == null || demanda.getStatusKanban().isVisivelExternamente();
    }

    private List<EtiquetaResponse> buscarEtiquetas(Integer demandaId) {
        return demandaEtiquetaRepository.findByDemandaIdComEtiqueta(demandaId).stream()
                .map(de -> EtiquetaResponse.from(de.getEtiqueta()))
                .toList();
    }

    private Map<Integer, List<EtiquetaResponse>> buscarEtiquetasPorDemanda(List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Map.of();
        }
        return demandaEtiquetaRepository.findByDemandaIdInComEtiqueta(demandaIds).stream()
                .collect(Collectors.groupingBy(
                        de -> de.getDemanda().getId(),
                        Collectors.mapping(de -> EtiquetaResponse.from(de.getEtiqueta()), Collectors.toList())));
    }

    /** Ids das demandas (dentre as informadas) que já têm pelo menos um anexo - pra
     * `DemandaResponse.temAnexos` em lote, sem N+1 (uma consulta só pra listagem inteira). */
    private Set<Integer> buscarIdsComAnexo(List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Set.of();
        }
        return documentoRepository.findByDemandaIdIn(demandaIds).stream()
                .map(documento -> documento.getDemanda().getId())
                .collect(Collectors.toSet());
    }

    /** Ids das demandas (dentre as informadas) que têm pelo menos uma nota pendente
     * (ainda não lida OU sem resposta) - pra `DemandaResponse.temNotaPendente` em lote,
     * sem N+1 (pedido do Romulo: ícone de alerta no card do Kanban). */
    private Set<Integer> buscarIdsComNotaPendente(List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Set.of();
        }
        Map<Integer, List<DemandaNota>> notasPorDemanda = notaRepository.findByDemandaIdIn(demandaIds).stream()
                .collect(Collectors.groupingBy(nota -> nota.getDemanda().getId()));
        Set<Integer> pendentes = new HashSet<>();
        notasPorDemanda.forEach((demandaId, notas) -> {
            if (algumaPendente(notas)) {
                pendentes.add(demandaId);
            }
        });
        return pendentes;
    }

    /** Resultado de {@link #calcularFlagsEtapas} - as duas informações que vêm da MESMA
     * lista de etapas de uma demanda, calculadas juntas pra não percorrer a lista duas
     * vezes. {@code vigente} pode ser true ao mesmo tempo que {@code vencida} (etapas
     * diferentes) - nesse caso o card prioriza o vermelho (ver `DemandaResponse.temEtapaVencida`). */
    private record EtapaFlags(boolean vencida, boolean vigente) {
        static final EtapaFlags NENHUMA = new EtapaFlags(false, false);
    }

    /** Ids → flags das demandas (dentre as informadas) com pelo menos uma etapa vencida
     * e/ou pelo menos uma etapa ainda vigente (prazo marcado, não vencido, não concluída) -
     * pra `DemandaResponse.temEtapaVencida`/`temEtapaVigente` em lote, sem N+1 (pedido do
     * Romulo: contorno vermelho/verde de destaque no card do Kanban). Chamada nos dois
     * branches de {@link #listar} - desde a v134 morador também vê a lista de etapas (só
     * leitura), então esse indicador deixou de ser exclusivo de funcionário (v136). */
    private Map<Integer, EtapaFlags> buscarFlagsEtapasPorDemanda(List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<DemandaEtapa>> etapasPorDemanda = etapaRepository.findByDemandaIdIn(demandaIds).stream()
                .collect(Collectors.groupingBy(et -> et.getDemanda().getId()));
        Map<Integer, EtapaFlags> flags = new HashMap<>();
        etapasPorDemanda.forEach((demandaId, etapas) -> flags.put(demandaId, calcularFlagsEtapas(etapas)));
        return flags;
    }

    /** Mesmo cálculo de {@link #buscarFlagsEtapasPorDemanda}, só que pra UMA demanda -
     * usada nos endpoints que retornam uma demanda isolada (aprovar/reprovar/mover/
     * alternar sigilo/arquivar), mesmo espírito de {@link #temNotaPendente}. */
    private EtapaFlags flagsEtapas(Integer demandaId) {
        return calcularFlagsEtapas(etapaRepository.findByDemandaIdOrderByOrdem(demandaId));
    }

    /** Ids → desde quando cada demanda está na coluna ATUAL dela, em lote (pra
     * {@code DemandaResponse.statusKanbanDesde}, KPI de "dias parado" do Kanban) - a
     * transição mais recente do histórico É a entrada na coluna atual (ver
     * {@link DemandaStatusKanbanHistoricoRepository#buscarUltimaTransicaoPorDemanda}).
     * Demanda sem histórico (nunca teve coluna) simplesmente não aparece no map. */
    private Map<Integer, LocalDateTime> buscarStatusKanbanDesdePorDemanda(List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, LocalDateTime> desde = new HashMap<>();
        for (Object[] linha : historicoRepository.buscarUltimaTransicaoPorDemanda(demandaIds)) {
            desde.put((Integer) linha[0], (LocalDateTime) linha[1]);
        }
        return desde;
    }

    /** Vencida = tem prazo marcado, o prazo já passou (antes de hoje) e ainda não foi
     * concluída. Vigente = tem prazo marcado, ainda não passou (hoje ou depois) e ainda
     * não foi concluída. Etapa sem prazo, ou já concluída, não conta pra nenhum dos dois. */
    private EtapaFlags calcularFlagsEtapas(List<DemandaEtapa> etapas) {
        LocalDate hoje = LocalDate.now();
        boolean vencida = false;
        boolean vigente = false;
        for (DemandaEtapa etapa : etapas) {
            if (etapa.isConcluida() || etapa.getPrazo() == null) {
                continue;
            }
            if (etapa.getPrazo().isBefore(hoje)) {
                vencida = true;
            } else {
                vigente = true;
            }
        }
        return new EtapaFlags(vencida, vigente);
    }

    /** Ids das demandas (dentre as informadas) que o MORADOR do contexto acompanha
     * (funcionalidade "Acompanhar", v116) - pra `DemandaResponse.acompanhando` em lote,
     * sem N+1. Só chamada no branch de morador de {@link #listar} - funcionário nunca
     * acompanha, sempre `false` direto (ver ali). */
    private Set<Integer> buscarIdsAcompanhados(Integer moradorId, List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Set.of();
        }
        return acompanhamentoRepository.findByMoradorIdAndDemandaIdIn(moradorId, demandaIds).stream()
                .map(a -> a.getDemanda().getId())
                .collect(Collectors.toSet());
    }

    /** Mesmo cálculo de {@link #buscarIdsComNotaPendente}, só que pra UMA demanda - usada
     * nos endpoints que retornam uma demanda isolada (criar/aprovar/reprovar/mover/alternar
     * sigilo), mesmo espírito de {@link #buscarResponsaveis}. */
    private boolean temNotaPendente(Integer demandaId) {
        return algumaPendente(notaRepository.findByDemandaIdOrderByCreatedAtAsc(demandaId));
    }

    /** Pendente = alguma nota RAIZ ({@code notaPai == null}) ainda não lida. "Lida" e
     * "respondida" são a MESMA coisa pro sistema (pedido do Romulo: "uma nota lida ou
     * respondida é uma nota lida") - marcar manualmente já basta, não é preciso além disso
     * ter uma resposta de verdade; responder também já marca a nota-pai como lida na mesma
     * tacada ({@code DemandaNotaService.criar}), então esse único critério cobre os dois
     * jeitos de resolver. Só olha notas RAIZ porque toda resposta já nasce lida sozinha
     * (v131, ver {@code DemandaNotaService.criar}) - nunca precisaria do filtro pra ela
     * mesma não contar, mas deixa explícito o que de fato importa aqui: só pergunta de
     * verdade pode estar pendente. */
    private boolean algumaPendente(List<DemandaNota> notas) {
        return notas.stream().filter(nota -> nota.getNotaPai() == null).anyMatch(nota -> !nota.isLida());
    }

    /** Responsáveis de UMA demanda só - usada nos endpoints que retornam uma demanda
     * isolada (criar/aprovar/reprovar/mover/alternar sigilo), onde não vale a pena montar
     * o mapa em lote de {@link #buscarResponsaveisPorDemanda}. */
    private List<ResponsavelResumoResponse> buscarResponsaveis(Integer demandaId) {
        return montarResumo(responsavelRepository.findByDemandaId(demandaId));
    }

    /** Responsáveis de várias demandas de uma vez - pra `listar()` (card do Kanban), sem
     * N+1: uma consulta pras atribuições + uma pros links de foto (mesmo padrão de
     * {@link #buscarEtiquetasPorDemanda}/{@link #buscarIdsComAnexo}). */
    private Map<Integer, List<ResponsavelResumoResponse>> buscarResponsaveisPorDemanda(List<Integer> demandaIds) {
        if (demandaIds.isEmpty()) {
            return Map.of();
        }
        List<DemandaResponsavel> atribuicoes = responsavelRepository.findByDemandaIdIn(demandaIds);
        Map<Integer, String> fotosPorFuncionario = pessoaFotoService.buscarUrls(
                atribuicoes.stream().map(a -> a.getFuncionario().getId()).distinct().toList());
        return atribuicoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDemanda().getId(),
                        Collectors.mapping(
                                a -> new ResponsavelResumoResponse(
                                        a.getFuncionario().getId(),
                                        a.getFuncionario().getNome(),
                                        fotosPorFuncionario.get(a.getFuncionario().getId())),
                                Collectors.toList())));
    }

    /** Mesmo mapeamento pra foto usado em {@link #buscarResponsaveisPorDemanda}, só que
     * pra uma lista de atribuições já de uma demanda só (ver {@link #buscarResponsaveis}). */
    private List<ResponsavelResumoResponse> montarResumo(List<DemandaResponsavel> atribuicoes) {
        Map<Integer, String> fotosPorFuncionario = pessoaFotoService.buscarUrls(
                atribuicoes.stream().map(a -> a.getFuncionario().getId()).distinct().toList());
        return atribuicoes.stream()
                .map(a -> new ResponsavelResumoResponse(
                        a.getFuncionario().getId(), a.getFuncionario().getNome(), fotosPorFuncionario.get(a.getFuncionario().getId())))
                .toList();
    }

    private void exigirPendente(Demanda demanda) {
        if (demanda.getStatusAprovacao() != DemandaStatusAprovacao.pendente) {
            throw new ConflictException(
                    "Essa demanda já foi " + demanda.getStatusAprovacao().name() + " - não dá pra decidir de novo");
        }
    }

    private void exigirFuncionarioDoCondominio(ContextoAutenticado contexto, Demanda demanda) {
        boolean autorizado = TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && demanda.getCondominio().getId().equals(contexto.condominioId());
        if (!autorizado) {
            throw new ForbiddenException("Só funcionário deste condomínio pode fazer isso");
        }
    }

    private void exigirFuncionarioOuMorador(ContextoAutenticado contexto) {
        if (!TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && !TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            throw new ForbiddenException("Só funcionário ou morador pode cadastrar/ver demanda");
        }
    }

    /** Funcionalidade "Acompanhar" (v116) é morador-only. */
    private void exigirMorador(ContextoAutenticado contexto) {
        if (!TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            throw new ForbiddenException("Só morador pode fazer isso");
        }
    }

    private Demanda buscarDemanda(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }

    private Funcionario buscarFuncionario(Integer id) {
        return funcionarioRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }

    private Morador buscarMorador(Integer id) {
        return moradorRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + id));
    }
}
