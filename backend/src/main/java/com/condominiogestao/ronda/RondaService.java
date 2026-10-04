package com.condominiogestao.ronda;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.demanda.DemandaRepository;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioPerfil;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.ronda.dto.RondaDetalheResponse;
import com.condominiogestao.ronda.dto.RondaFinalizarRequest;
import com.condominiogestao.ronda.dto.RondaPontoRequest;
import com.condominiogestao.ronda.dto.RondaResponse;
import com.condominiogestao.ronda.dto.RondaResumoResponse;
import com.condominiogestao.security.ContextoAutenticado;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feature "Controle de Rondas" (pedido do Romulo). A ronda é "livre": sem pontos de
 * controle nem rota cadastrada, o sistema só registra o que de fato aconteceu (início,
 * fim, trajeto via {@link RondaPonto}, demandas abertas durante ela).
 *
 * <p>"Só uma ronda ativa por vez" é por rondista, nunca por condomínio - vários rondistas
 * do mesmo condomínio podem estar cada um com sua própria ronda {@code em_andamento} ao
 * mesmo tempo, sem conflito.
 *
 * <p>Encerramento: três jeitos (ver {@link RondaStatus}), sempre com a mesma lógica de
 * fechamento ({@link #fecharRonda}) - só o status resultante e quem disparou mudam. Não há
 * nenhum job agendado no projeto hoje - em vez de criar essa infraestrutura só pra isso,
 * {@link #fecharSeAbandonada} é chamado sob demanda em toda leitura que tocar uma ronda
 * {@code em_andamento} iniciada há mais de {@value #HORAS_ABANDONO} horas.
 */
@Service
@Transactional(readOnly = true)
public class RondaService {

    private static final int HORAS_ABANDONO = 12;

    private final RondaRepository repository;
    private final RondaPontoRepository pontoRepository;
    private final CondominioRepository condominioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final DemandaRepository demandaRepository;

    public RondaService(
            RondaRepository repository,
            RondaPontoRepository pontoRepository,
            CondominioRepository condominioRepository,
            FuncionarioRepository funcionarioRepository,
            DemandaRepository demandaRepository) {
        this.repository = repository;
        this.pontoRepository = pontoRepository;
        this.condominioRepository = condominioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.demandaRepository = demandaRepository;
    }

    @Transactional
    public RondaResponse iniciar(ContextoAutenticado contexto) {
        exigirRondista(contexto);

        repository.findByFuncionarioIdAndStatus(contexto.pessoaId(), RondaStatus.em_andamento).ifPresent(ativa -> {
            fecharSeAbandonada(ativa);
            if (ativa.getStatus() == RondaStatus.em_andamento) {
                throw new ConflictException("Você já tem uma ronda em andamento");
            }
        });

        Condominio condominio = condominioRepository
                .findById(contexto.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + contexto.condominioId()));
        Funcionario funcionario = buscarFuncionario(contexto.pessoaId());

        Ronda ronda = new Ronda();
        ronda.setCondominio(condominio);
        ronda.setFuncionario(funcionario);
        ronda.setIniciadaEm(LocalDateTime.now());
        ronda.setStatus(RondaStatus.em_andamento);

        Ronda salva = repository.save(ronda);
        return RondaResponse.from(salva, 0, podeVerNomeFuncionario(contexto), podeVerObservacao(contexto));
    }

    @Transactional
    public RondaResponse buscarAtiva(ContextoAutenticado contexto) {
        exigirRondista(contexto);
        Ronda ronda = repository
                .findByFuncionarioIdAndStatus(contexto.pessoaId(), RondaStatus.em_andamento)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma ronda em andamento"));
        fecharSeAbandonada(ronda);
        if (ronda.getStatus() != RondaStatus.em_andamento) {
            throw new ResourceNotFoundException("Nenhuma ronda em andamento");
        }
        return RondaResponse.from(
                ronda, demandaRepository.countByRondaIdIn(List.of(ronda.getId())), podeVerNomeFuncionario(contexto),
                podeVerObservacao(contexto));
    }

    @Transactional
    public void registrarPontos(ContextoAutenticado contexto, Integer rondaId, List<RondaPontoRequest> pontos) {
        Ronda ronda = buscarRonda(rondaId);
        exigirDono(contexto, ronda);
        exigirEmAndamento(ronda);

        List<RondaPonto> entidades = pontos.stream().map(p -> {
            RondaPonto ponto = new RondaPonto();
            ponto.setRonda(ronda);
            ponto.setLatitude(p.latitude());
            ponto.setLongitude(p.longitude());
            ponto.setCapturadoEm(LocalDateTime.ofInstant(p.capturadoEm(), ZoneId.systemDefault()));
            return ponto;
        }).toList();
        pontoRepository.saveAll(entidades);
    }

    /**
     * Autorizado pro próprio dono da ronda (encerramento normal, {@code finalizada}) OU
     * por um perfil completo do condomínio encerrando uma ronda esquecida de outro
     * rondista ({@code encerrada_manualmente}) - pedido do Romulo.
     *
     * <p>Quando é o dono, {@code request.tipo()} é obrigatório (o modal do "Finalizar ronda"
     * já força escolher) e {@code request.observacao()} é opcional; quando é um perfil
     * completo encerrando a ronda de outra pessoa, o corpo é ignorado (só o rondista sabe o
     * que fez) - a ronda fica sem tipo/observação.
     */
    @Transactional
    public RondaResponse finalizar(ContextoAutenticado contexto, Integer rondaId, RondaFinalizarRequest request) {
        Ronda ronda = buscarRonda(rondaId);
        boolean ehDono = ronda.getFuncionario().getId().equals(contexto.pessoaId());
        boolean ehGestor = Autorizacao.ehPerfilCompletoDoCondominio(contexto, ronda.getCondominio().getId());
        if (!ehDono && !ehGestor) {
            throw new ForbiddenException("Só o próprio rondista ou um perfil completo deste condomínio pode finalizar a ronda");
        }
        exigirEmAndamento(ronda);

        if (ehDono) {
            if (request == null || request.tipo() == null) {
                throw new InvalidRequestException("Selecione o tipo da ronda");
            }
            ronda.setTipo(request.tipo());
            String observacao = request.observacao() == null ? "" : request.observacao().trim();
            ronda.setObservacao(observacao.isEmpty() ? null : observacao);
        }

        fecharRonda(
                ronda,
                ehDono ? RondaStatus.finalizada : RondaStatus.encerrada_manualmente,
                LocalDateTime.now(),
                pontoRepository.findByRondaIdOrderByCapturadoEmAsc(ronda.getId()));
        Ronda salva = repository.save(ronda);
        return RondaResponse.from(
                salva, demandaRepository.countByRondaIdIn(List.of(salva.getId())), podeVerNomeFuncionario(contexto),
                podeVerObservacao(contexto));
    }

    public PaginaResponse<RondaResponse> listarPagina(
            ContextoAutenticado contexto,
            Integer funcionarioId,
            LocalDateTime inicio,
            LocalDateTime fim,
            TipoRonda tipo,
            Integer rondaId,
            String observacao,
            int pagina,
            int tamanho) {
        exigirPodeVerHistorico(contexto);

        Page<Ronda> paginaRondas = repository.findAll(
                especificacao(
                        contexto.condominioId(),
                        funcionarioIdEfetivo(contexto, funcionarioId),
                        inicio,
                        fim,
                        tipo,
                        rondaId,
                        observacaoEfetiva(contexto, observacao)),
                PageRequest.of(
                        Math.max(pagina, 0),
                        Math.min(Math.max(tamanho, 1), 100),
                        Sort.by(Sort.Direction.DESC, "iniciadaEm")));
        paginaRondas.forEach(this::fecharSeAbandonada);

        Map<Integer, Long> totalDemandasPorRonda = contarDemandasPorRonda(paginaRondas.getContent());
        boolean podeVerNome = podeVerNomeFuncionario(contexto);
        boolean podeVerObs = podeVerObservacao(contexto);
        return PaginaResponse.from(paginaRondas.map(ronda -> RondaResponse.from(
                ronda, totalDemandasPorRonda.getOrDefault(ronda.getId(), 0L), podeVerNome, podeVerObs)));
    }

    /** Teto de segurança pro {@code findAll} sem paginação abaixo - perfil completo pode
     * filtrar "resumo" sem período (padrão do filtro pra esse papel, ver tela Rondas), e sem
     * teto isso carregaria toda a tabela `rondas` do condomínio na memória. Um condomínio
     * passando disso num resumo sem filtro de período vê o total subcontado - aceitável pra
     * esse card de KPI, que não é a fonte de verdade (a lista paginada mostra tudo). */
    private static final int LIMITE_RONDAS_RESUMO = 5000;

    public RondaResumoResponse resumo(
            ContextoAutenticado contexto,
            Integer funcionarioId,
            LocalDateTime inicio,
            LocalDateTime fim,
            TipoRonda tipo,
            Integer rondaId,
            String observacao) {
        exigirPodeVerHistorico(contexto);

        List<Ronda> rondas = repository.findAll(
                        especificacao(
                                contexto.condominioId(),
                                funcionarioIdEfetivo(contexto, funcionarioId),
                                inicio,
                                fim,
                                tipo,
                                rondaId,
                                observacaoEfetiva(contexto, observacao)),
                        PageRequest.of(0, LIMITE_RONDAS_RESUMO, Sort.by(Sort.Direction.DESC, "iniciadaEm")))
                .getContent();
        rondas.forEach(this::fecharSeAbandonada);

        LocalDateTime agora = LocalDateTime.now();
        long tempoTotalSegundos = rondas.stream()
                .mapToLong(r -> java.time.Duration.between(
                                r.getIniciadaEm(), r.getFinalizadaEm() != null ? r.getFinalizadaEm() : agora)
                        .getSeconds())
                .sum();
        long totalDemandas = demandaRepository.countByRondaIdIn(rondas.stream().map(Ronda::getId).toList());

        return new RondaResumoResponse(rondas.size(), tempoTotalSegundos, totalDemandas);
    }

    public RondaDetalheResponse buscarDetalhe(ContextoAutenticado contexto, Integer rondaId) {
        Ronda ronda = buscarRonda(rondaId);
        boolean ehDono = ronda.getFuncionario().getId().equals(contexto.pessoaId());
        boolean autorizado = ehDono
                || Autorizacao.ehPerfilCompletoDoCondominio(contexto, ronda.getCondominio().getId())
                || Autorizacao.ehMoradorDoCondominio(contexto, ronda.getCondominio().getId());
        if (!autorizado) {
            throw new ForbiddenException("Só o próprio rondista, um morador ou um perfil completo deste condomínio pode ver essa ronda");
        }
        fecharSeAbandonada(ronda);

        List<RondaPonto> pontos = pontoRepository.findByRondaIdOrderByCapturadoEmAsc(rondaId);
        long totalDemandas = demandaRepository.countByRondaIdIn(List.of(ronda.getId()));
        return RondaDetalheResponse.from(
                ronda, totalDemandas, pontos, podeVerNomeFuncionario(contexto), podeVerObservacao(contexto));
    }

    /** Filtros opcionais da tela "Rondas" do síndico - `condominioId` sempre presente (é o
     * do próprio contexto, nunca vem da requisição), `funcionarioId`/`inicio`/`fim` só
     * viram predicado quando informados (ver Javadoc de {@link RondaRepository} - é
     * exatamente pra evitar um parâmetro nulo sem tipo chegar ao Postgres). */
    private Specification<Ronda> especificacao(
            Integer condominioId,
            Integer funcionarioId,
            LocalDateTime inicio,
            LocalDateTime fim,
            TipoRonda tipo,
            Integer rondaId,
            String observacao) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("condominio").get("id"), condominioId));
            if (funcionarioId != null) {
                predicados.add(cb.equal(root.get("funcionario").get("id"), funcionarioId));
            }
            if (tipo != null) {
                predicados.add(cb.equal(root.get("tipo"), tipo));
            }
            if (rondaId != null) {
                predicados.add(cb.equal(root.get("id"), rondaId));
            }
            if (observacao != null) {
                predicados.add(cb.like(cb.lower(root.get("observacao")), "%" + escaparLike(observacao.toLowerCase()) + "%", '\\'));
            }
            if (inicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("iniciadaEm"), inicio));
            }
            if (fim != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("iniciadaEm"), fim));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /** Rede de segurança final (sem job agendado - ver Javadoc da classe): se ninguém
     * fechou a ronda (nem o rondista, nem um gestor) e já se passaram
     * {@value #HORAS_ABANDONO}h desde o início, encerra sozinha na hora em que qualquer
     * leitura tocar essa ronda. Usa o horário do último ponto de GPS como fim, se houver -
     * mais fiel ao que de fato aconteceu do que "agora". */
    private void fecharSeAbandonada(Ronda ronda) {
        if (ronda.getStatus() != RondaStatus.em_andamento) {
            return;
        }
        if (ronda.getIniciadaEm().isAfter(LocalDateTime.now().minusHours(HORAS_ABANDONO))) {
            return;
        }
        List<RondaPonto> pontos = pontoRepository.findByRondaIdOrderByCapturadoEmAsc(ronda.getId());
        LocalDateTime fim = pontos.isEmpty() ? ronda.getIniciadaEm() : pontos.get(pontos.size() - 1).getCapturadoEm();
        fecharRonda(ronda, RondaStatus.encerrada_automaticamente, fim, pontos);
        repository.save(ronda);
    }

    private void fecharRonda(Ronda ronda, RondaStatus status, LocalDateTime finalizadaEm, List<RondaPonto> pontos) {
        ronda.setFinalizadaEm(finalizadaEm);
        ronda.setStatus(status);
        ronda.setDistanciaMetros(calcularDistanciaMetros(pontos));
    }

    private double calcularDistanciaMetros(List<RondaPonto> pontosOrdenados) {
        double totalMetros = 0;
        for (int i = 1; i < pontosOrdenados.size(); i++) {
            totalMetros += distanciaHaversineMetros(pontosOrdenados.get(i - 1), pontosOrdenados.get(i));
        }
        return totalMetros;
    }

    /** Distância em linha reta entre dois pontos de GPS (fórmula de Haversine, raio médio
     * da Terra em metros) - soma entre pontos consecutivos aproxima o trajeto percorrido. */
    private double distanciaHaversineMetros(RondaPonto a, RondaPonto b) {
        double raioTerraMetros = 6_371_000;
        double deltaLatRad = Math.toRadians(b.getLatitude() - a.getLatitude());
        double deltaLonRad = Math.toRadians(b.getLongitude() - a.getLongitude());
        double haversine = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                        * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double anguloCentral = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return raioTerraMetros * anguloCentral;
    }

    private Map<Integer, Long> contarDemandasPorRonda(List<Ronda> rondas) {
        List<Integer> ids = rondas.stream().map(Ronda::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Long> contagens = new HashMap<>();
        for (Object[] linha : demandaRepository.contarPorRondaId(ids)) {
            contagens.put((Integer) linha[0], (Long) linha[1]);
        }
        return contagens;
    }

    /** Histórico de rondas (tela "Rondas") tem 3 públicos, com escopo diferente cada
     * (pedido do Romulo): perfil completo vê tudo do condomínio; rondista vê só as
     * PRÓPRIAS (ver {@link #funcionarioIdEfetivo}); morador do condomínio também vê tudo
     * (transparência - sem poder encerrar nada, isso continua só perfil completo/dono em
     * {@link #finalizar}). */
    private void exigirPodeVerHistorico(ContextoAutenticado contexto) {
        boolean autorizado = Autorizacao.ehPerfilCompletoDoCondominio(contexto, contexto.condominioId())
                || ehRondista(contexto)
                || Autorizacao.ehMoradorDoCondominio(contexto, contexto.condominioId());
        if (!autorizado) {
            throw new ForbiddenException("Só funcionário ou morador deste condomínio pode ver o histórico de rondas");
        }
    }

    /** Rondista só vê as PRÓPRIAS rondas no histórico - ignora qualquer `funcionarioId`
     * vindo da requisição e força o próprio id, pra um rondista não conseguir ver a ronda
     * de outro só trocando o parâmetro na chamada. Perfil completo/morador usam o filtro
     * como veio (`null` = todos os rondistas do condomínio). */
    private Integer funcionarioIdEfetivo(ContextoAutenticado contexto, Integer funcionarioIdSolicitado) {
        return ehRondista(contexto) ? contexto.pessoaId() : funcionarioIdSolicitado;
    }

    /** Pedido do Romulo: só perfil completo vê QUEM fez a ronda e a observação escrita pelo
     * rondista - rondista (a própria lista) e morador (transparência) veem só o {@code id}
     * e o {@code tipo} da ronda. */
    private boolean podeVerNomeFuncionario(ContextoAutenticado contexto) {
        return Autorizacao.ehPerfilCompletoDoCondominio(contexto, contexto.condominioId());
    }

    /** Observação escrita pelo rondista: perfil completo e o próprio rondista (lembrete pra
     * ele - a lista dele já é só das próprias rondas, ver {@link #funcionarioIdEfetivo}).
     * Morador não vê. */
    private boolean podeVerObservacao(ContextoAutenticado contexto) {
        return podeVerNomeFuncionario(contexto) || ehRondista(contexto);
    }

    /** Filtro por texto da observação só vale pra quem PODE ver a observação - senão um
     * morador descobriria o que o rondista escreveu testando trechos no filtro. Pra ele o
     * parâmetro é ignorado (não dá erro, só não filtra). Vazio/em branco também não filtra. */
    private String observacaoEfetiva(ContextoAutenticado contexto, String observacao) {
        if (!podeVerObservacao(contexto) || observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }

    /** `%` e `_` digitados no filtro valem como texto, não como curinga do LIKE. */
    private String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean ehRondista(ContextoAutenticado contexto) {
        return TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && FuncionarioPerfil.rondista.name().equals(contexto.perfil());
    }

    private void exigirRondista(ContextoAutenticado contexto) {
        if (!ehRondista(contexto)) {
            throw new ForbiddenException("Só rondista pode fazer isso");
        }
    }

    private void exigirDono(ContextoAutenticado contexto, Ronda ronda) {
        if (!ronda.getFuncionario().getId().equals(contexto.pessoaId())) {
            throw new ForbiddenException("Só o próprio rondista pode fazer isso");
        }
    }

    private void exigirEmAndamento(Ronda ronda) {
        if (ronda.getStatus() != RondaStatus.em_andamento) {
            throw new ConflictException("Essa ronda já foi encerrada");
        }
    }

    private Ronda buscarRonda(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ronda não encontrada: " + id));
    }

    private Funcionario buscarFuncionario(Integer id) {
        return funcionarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }
}
