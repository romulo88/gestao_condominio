package com.condominiogestao.evento;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ForbiddenException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.PaginaResponse;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.espacocomum.EspacoComum;
import com.condominiogestao.espacocomum.EspacoComumRepository;
import com.condominiogestao.evento.dto.EventoCreateRequest;
import com.condominiogestao.evento.dto.EventoPessoaAtualizarRequest;
import com.condominiogestao.evento.dto.EventoPessoaCandidatoResponse;
import com.condominiogestao.evento.dto.EventoResponse;
import com.condominiogestao.evento.dto.EventoUpdateRequest;
import com.condominiogestao.evento.dto.EventoVeiculoAtualizarRequest;
import com.condominiogestao.evento.dto.EventoVeiculoCandidatoResponse;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.funcionario.FuncionarioRepository;
import com.condominiogestao.morador.Morador;
import com.condominiogestao.morador.MoradorCondominioRepository;
import com.condominiogestao.morador.MoradorRepository;
import com.condominiogestao.parametro.ParametroService;
import com.condominiogestao.security.ContextoAutenticado;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cadastro de eventos pelo morador (festa/visita, pedido do Romulo) - substitui o aviso
 * por WhatsApp pra portaria. Não é sistema de reserva: sem checagem de conflito entre
 * eventos no mesmo espaço/dia. Liberação é por item (pessoa/veículo, não por evento
 * inteiro) e funciona como um toggle - liberar de novo desfaz, corrige engano do porteiro
 * sem endpoint separado.
 */
@Service
@Transactional(readOnly = true)
public class EventoService {

    private final EventoRepository repository;
    private final EventoVeiculoRepository veiculoRepository;
    private final EventoPessoaRepository pessoaRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorRepository moradorRepository;
    private final MoradorCondominioRepository moradorCondominioRepository;
    private final EspacoComumRepository espacoComumRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ParametroService parametroService;
    private final MinioClient minioClient;
    private final String bucket;

    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int FOTO_TAMANHO_MAXIMO_MB_PADRAO = 8;

    public EventoService(
            EventoRepository repository,
            EventoVeiculoRepository veiculoRepository,
            EventoPessoaRepository pessoaRepository,
            CondominioRepository condominioRepository,
            MoradorRepository moradorRepository,
            MoradorCondominioRepository moradorCondominioRepository,
            EspacoComumRepository espacoComumRepository,
            FuncionarioRepository funcionarioRepository,
            ParametroService parametroService,
            MinioClient minioClient,
            @Value("${storage.bucket}") String bucket) {
        this.repository = repository;
        this.veiculoRepository = veiculoRepository;
        this.pessoaRepository = pessoaRepository;
        this.condominioRepository = condominioRepository;
        this.moradorRepository = moradorRepository;
        this.moradorCondominioRepository = moradorCondominioRepository;
        this.espacoComumRepository = espacoComumRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.parametroService = parametroService;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * {@code reforma} (pedido do Romulo) gera um {@link Evento} por dia do intervalo
     * {@code data}..{@code dataFim} (ambos inclusive), cada um repetindo motivo/horario/
     * veiculos/pessoas - não existe "evento de vários dias" no modelo, então cada dia
     * liberável pela portaria precisa do seu próprio registro. Sem reforma, é só o
     * comportamento de sempre (lista com 1 evento).
     */
    @Transactional
    public List<EventoResponse> criar(ContextoAutenticado contexto, EventoCreateRequest request) {
        exigirMorador(contexto);

        if (request.data().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("A data do evento não pode ser no passado");
        }
        List<LocalDate> datas = resolverDatasReforma(request);

        Condominio condominio = condominioRepository
                .findById(contexto.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + contexto.condominioId()));
        Morador morador = moradorRepository
                .findById(contexto.pessoaId())
                .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado: " + contexto.pessoaId()));
        EspacoComum espacoComum = resolverEspacoComum(request.espacoComumId(), contexto.condominioId());
        String horario = request.horario() == null || request.horario().isBlank() ? null : request.horario().trim();
        String unidade = buscarUnidade(morador.getId(), contexto.condominioId());
        // "Visitante recorrente" (pedido do Romulo): calculado UMA VEZ, antes do laço de
        // dias da reforma, só a partir do histórico já existente no banco - cada dia novo
        // herda do histórico antigo, nunca de um dia-irmão criado nesta mesma chamada.
        Map<String, EventoPessoa> fotosRecentes = mapaFotosRecentes(morador.getId());

        List<EventoResponse> respostas = new ArrayList<>();
        for (LocalDate data : datas) {
            Evento evento = new Evento();
            evento.setCondominio(condominio);
            evento.setMorador(morador);
            evento.setMotivo(request.motivo());
            evento.setData(data);
            evento.setHorario(horario);
            evento.setEspacoComum(espacoComum);
            evento.setReforma(request.reforma());

            Evento salvo = repository.save(evento);

            List<EventoVeiculo> veiculos = request.veiculos().stream().map(v -> {
                EventoVeiculo veiculo = new EventoVeiculo();
                veiculo.setEvento(salvo);
                veiculo.setPlaca(v.placa().trim().toUpperCase());
                return veiculo;
            }).toList();
            veiculoRepository.saveAll(veiculos);

            List<EventoPessoa> pessoas = request.pessoas().stream().map(p -> {
                EventoPessoa pessoa = new EventoPessoa();
                pessoa.setEvento(salvo);
                pessoa.setNome(p.nome().trim());
                pessoa.setDocumento(p.documento().trim());
                return pessoa;
            }).toList();
            pessoaRepository.saveAll(pessoas);
            pessoas.forEach(pessoa -> copiarFotoHistorica(pessoa, fotosRecentes));

            // Morador nunca vê fotoUrl - nem faz sentido aqui, evento recém-criado não tem foto ainda.
            respostas.add(EventoResponse.from(salvo, unidade, veiculos, pessoas, false));
        }
        return respostas;
    }

    /** Sem {@code reforma}: só a própria {@code data}. Com {@code reforma}: exige unidade
     * própria ({@code espacoComumId} nulo), {@code dataFim} informada, não anterior à
     * {@code data}, e intervalo entre as duas de até 15 dias - devolve cada dia do
     * intervalo, início e fim inclusive. */
    private List<LocalDate> resolverDatasReforma(EventoCreateRequest request) {
        if (!request.reforma()) {
            return List.of(request.data());
        }
        if (request.espacoComumId() != null) {
            throw new InvalidRequestException("Reforma só pode ser cadastrada na própria unidade");
        }
        if (request.dataFim() == null) {
            throw new InvalidRequestException("Informe a data de fim da reforma");
        }
        if (request.dataFim().isBefore(request.data())) {
            throw new InvalidRequestException("A data de fim não pode ser anterior à data de início");
        }
        if (ChronoUnit.DAYS.between(request.data(), request.dataFim()) > 15) {
            throw new InvalidRequestException("O intervalo entre início e fim da reforma não pode passar de 15 dias");
        }
        return request.data().datesUntil(request.dataFim().plusDays(1)).toList();
    }

    /**
     * Só o próprio morador dono do evento, e só enquanto o evento ainda não ocorreu (pedido
     * do Romulo: "eu preciso editar um evento ainda futuro. Ponto.") - diferente da versão
     * anterior, item já liberado pela portaria NÃO bloqueia mais editar o evento inteiro,
     * só bloqueia editar AQUELE item específico (ver {@link #sincronizarPessoas}/
     * {@link #sincronizarVeiculos} - preservam item liberado intacto, ignorando qualquer
     * mudança nele).
     *
     * <p>Local e data só podem mudar se a data ATUAL (antes da edição) ainda não é hoje -
     * pedido do Romulo: "se a data do evento for igual a hoje, eu não posso mais trocar a
     * data e nem o espaço [...] caso seja futuro, eu posso mudar os dois". No dia do evento,
     * {@code request.espacoComumId()}/{@code request.data()} precisam vir iguais ao que já
     * está salvo (senão 400) - o formulário já trava esses campos nesse caso, então só
     * aconteceria via chamada direta à API.
     */
    @Transactional
    public EventoResponse atualizar(ContextoAutenticado contexto, Integer eventoId, EventoUpdateRequest request) {
        exigirMorador(contexto);
        Evento evento = buscarEvento(eventoId);
        exigirDonoDoEventoFuturo(contexto, evento, "editar");

        boolean eventoEhHoje = evento.getData().isEqual(LocalDate.now());
        if (eventoEhHoje) {
            Integer espacoAtualId = evento.getEspacoComum() == null ? null : evento.getEspacoComum().getId();
            boolean espacoMudou = !Objects.equals(espacoAtualId, request.espacoComumId());
            if (!request.data().isEqual(evento.getData()) || espacoMudou) {
                throw new InvalidRequestException("No dia do evento não é possível mudar a data nem o local");
            }
        } else {
            if (request.data().isBefore(LocalDate.now())) {
                throw new InvalidRequestException("A data do evento não pode ser no passado");
            }
            if (evento.isReforma() && request.espacoComumId() != null) {
                throw new InvalidRequestException("Evento de reforma é sempre na própria unidade");
            }
            evento.setData(request.data());
            evento.setEspacoComum(resolverEspacoComum(request.espacoComumId(), contexto.condominioId()));
        }

        evento.setMotivo(request.motivo());
        evento.setHorario(request.horario() == null || request.horario().isBlank() ? null : request.horario().trim());
        repository.save(evento);

        sincronizarVeiculos(evento, request.veiculos());
        long totalPessoas = sincronizarPessoas(evento, request.pessoas());
        if (totalPessoas == 0) {
            throw new InvalidRequestException("informe ao menos 1 pessoa");
        }

        return buscarDetalhe(contexto, eventoId);
    }

    /** Sincroniza a lista de pessoas do evento com o que veio no request, SEM tocar em
     * quem já foi liberado pela portaria (preservado intacto mesmo que não venha na
     * lista, e qualquer mudança nele é ignorada silenciosamente - o formulário já
     * desabilita esses campos, então só aconteceria via chamada direta à API). Item com
     * {@code id} vira update; sem {@code id} vira criação; item não-liberado existente que
     * não veio na lista é removido. Devolve o total de pessoas do evento depois da
     * sincronização (liberadas + as desta lista), pra {@link #atualizar} conferir o mínimo
     * de 1. */
    private long sincronizarPessoas(Evento evento, List<EventoPessoaAtualizarRequest> itens) {
        List<EventoPessoa> existentes = pessoaRepository.findByEventoIdOrderById(evento.getId());
        Map<Integer, EventoPessoa> existentesPorId = new HashMap<>();
        for (EventoPessoa p : existentes) {
            existentesPorId.put(p.getId(), p);
        }
        // "Visitante recorrente" (pedido do Romulo) - só pra pessoa NOVA (sem id), mesmo
        // critério de {@link #criar}; editar uma pessoa já existente pra bater com outro
        // nome/documento do histórico não dispara isso, fora do escopo do pedido original.
        Map<String, EventoPessoa> fotosRecentes = mapaFotosRecentes(evento.getMorador().getId());

        Set<Integer> mantidos = new HashSet<>();
        for (EventoPessoaAtualizarRequest item : itens) {
            if (item.id() != null) {
                EventoPessoa existente = existentesPorId.get(item.id());
                if (existente == null) {
                    throw new ResourceNotFoundException("Pessoa não encontrada nesse evento: " + item.id());
                }
                if (!existente.isLiberado()) {
                    existente.setNome(item.nome().trim());
                    existente.setDocumento(item.documento().trim());
                    pessoaRepository.save(existente);
                }
                mantidos.add(item.id());
            } else {
                EventoPessoa nova = new EventoPessoa();
                nova.setEvento(evento);
                nova.setNome(item.nome().trim());
                nova.setDocumento(item.documento().trim());
                pessoaRepository.save(nova);
                copiarFotoHistorica(nova, fotosRecentes);
            }
        }
        for (EventoPessoa existente : existentes) {
            if (!existente.isLiberado() && !mantidos.contains(existente.getId())) {
                pessoaRepository.delete(existente);
            }
        }

        return pessoaRepository.findByEventoIdOrderById(evento.getId()).size();
    }

    /** Mesmo espírito de {@link #sincronizarPessoas}, pra veículo (sem mínimo exigido). */
    private void sincronizarVeiculos(Evento evento, List<EventoVeiculoAtualizarRequest> itens) {
        List<EventoVeiculo> existentes = veiculoRepository.findByEventoIdOrderById(evento.getId());
        Map<Integer, EventoVeiculo> existentesPorId = new HashMap<>();
        for (EventoVeiculo v : existentes) {
            existentesPorId.put(v.getId(), v);
        }

        Set<Integer> mantidos = new HashSet<>();
        for (EventoVeiculoAtualizarRequest item : itens) {
            if (item.id() != null) {
                EventoVeiculo existente = existentesPorId.get(item.id());
                if (existente == null) {
                    throw new ResourceNotFoundException("Veículo não encontrado nesse evento: " + item.id());
                }
                if (!existente.isLiberado()) {
                    existente.setPlaca(item.placa().trim().toUpperCase());
                    veiculoRepository.save(existente);
                }
                mantidos.add(item.id());
            } else {
                EventoVeiculo novo = new EventoVeiculo();
                novo.setEvento(evento);
                novo.setPlaca(item.placa().trim().toUpperCase());
                veiculoRepository.save(novo);
            }
        }
        for (EventoVeiculo existente : existentes) {
            if (!existente.isLiberado() && !mantidos.contains(existente.getId())) {
                veiculoRepository.delete(existente);
            }
        }
    }

    /** Só o próprio morador dono do evento, e só enquanto (a) a data do evento ainda não
     * passou e (b) nenhum veículo/pessoa já foi liberado pela portaria - apagar o evento
     * inteiro depois disso destruiria um registro de liberação já feito (pedido do Romulo:
     * "apagar evento que ainda não ocorreu"). Apaga os filhos antes do próprio evento (FK). */
    @Transactional
    public void excluir(ContextoAutenticado contexto, Integer eventoId) {
        exigirMorador(contexto);
        Evento evento = buscarEvento(eventoId);
        exigirDonoEditavel(contexto, evento, "excluir");

        pessoaRepository.deleteAll(pessoaRepository.findByEventoIdOrderById(eventoId));
        veiculoRepository.deleteAll(veiculoRepository.findByEventoIdOrderById(eventoId));
        repository.delete(evento);
    }

    /** "Meus eventos" do morador - mais recente primeiro, sem paginação (baixo volume por morador).
     * Veículos/pessoas de todos os eventos em 2 queries em lote, não uma por evento - mesmo
     * espírito de {@link #montarRespostasEmLote}. */
    public List<EventoResponse> listarMeusEventos(ContextoAutenticado contexto) {
        exigirMorador(contexto);
        List<Evento> eventos = repository.findByMoradorIdOrderByDataDesc(contexto.pessoaId());
        if (eventos.isEmpty()) {
            return List.of();
        }
        String unidade = buscarUnidade(contexto.pessoaId(), contexto.condominioId());

        List<Integer> ids = eventos.stream().map(Evento::getId).toList();
        Map<Integer, List<EventoVeiculo>> veiculosPorEvento = new HashMap<>();
        for (EventoVeiculo v : veiculoRepository.findByEventoIdInOrderById(ids)) {
            veiculosPorEvento.computeIfAbsent(v.getEvento().getId(), k -> new ArrayList<>()).add(v);
        }
        Map<Integer, List<EventoPessoa>> pessoasPorEvento = new HashMap<>();
        for (EventoPessoa p : pessoaRepository.findByEventoIdInOrderById(ids)) {
            pessoasPorEvento.computeIfAbsent(p.getEvento().getId(), k -> new ArrayList<>()).add(p);
        }

        // Morador nunca vê fotoUrl, mesmo nos próprios eventos.
        return eventos.stream()
                .map(evento -> EventoResponse.from(
                        evento,
                        unidade,
                        veiculosPorEvento.getOrDefault(evento.getId(), List.of()),
                        pessoasPorEvento.getOrDefault(evento.getId(), List.of()),
                        false))
                .toList();
    }

    /** Calendário da portaria - perfil `porteiro` ou perfil completo do condomínio.
     * {@code dataInicio}/{@code dataFim}/{@code espacoComumId} são todos opcionais. */
    public PaginaResponse<EventoResponse> listarPagina(
            ContextoAutenticado contexto,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer espacoComumId,
            int pagina,
            int tamanho) {
        Autorizacao.exigirPorteiroOuPerfilCompleto(contexto, contexto.condominioId());

        Page<Evento> paginaEventos = repository.findAll(
                especificacao(contexto.condominioId(), dataInicio, dataFim, espacoComumId),
                PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100), Sort.by(Sort.Direction.ASC, "data")));

        List<EventoResponse> itens = montarRespostasEmLote(paginaEventos.getContent());
        return new PaginaResponse<>(itens, paginaEventos.getNumber(), paginaEventos.getTotalPages(), paginaEventos.getTotalElements());
    }

    public EventoResponse buscarDetalhe(ContextoAutenticado contexto, Integer eventoId) {
        Evento evento = buscarEvento(eventoId);
        boolean ehDono = evento.getMorador().getId().equals(contexto.pessoaId())
                && TipoPessoa.morador.name().equals(contexto.tipoPapel());
        boolean podeVerFoto = Autorizacao.ehPorteiroOuPerfilCompleto(contexto, evento.getCondominio().getId());
        if (!ehDono && !podeVerFoto) {
            throw new ForbiddenException("Só o próprio morador, o porteiro ou um perfil completo deste condomínio pode ver esse evento");
        }

        List<EventoVeiculo> veiculos = veiculoRepository.findByEventoIdOrderById(eventoId);
        List<EventoPessoa> pessoas = pessoaRepository.findByEventoIdOrderById(eventoId);
        String unidade = buscarUnidade(evento.getMorador().getId(), evento.getCondominio().getId());
        // podeVerFoto é sempre "é porteiro/perfil completo", nunca "é dono" - o morador não
        // vê a foto nem no próprio evento (ver EventoPessoaResponse).
        return EventoResponse.from(evento, unidade, veiculos, pessoas, podeVerFoto);
    }

    @Transactional
    public EventoResponse liberarVeiculo(ContextoAutenticado contexto, Integer eventoId, Integer veiculoId) {
        Evento evento = buscarEvento(eventoId);
        Autorizacao.exigirPorteiroOuPerfilCompleto(contexto, evento.getCondominio().getId());

        EventoVeiculo veiculo = veiculoRepository
                .findById(veiculoId)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado: " + veiculoId));
        if (!veiculo.getEvento().getId().equals(eventoId)) {
            throw new ResourceNotFoundException("Veículo não encontrado nesse evento: " + veiculoId);
        }

        boolean novoValor = !veiculo.isLiberado();
        veiculo.setLiberado(novoValor);
        veiculo.setFuncionarioLiberou(novoValor ? buscarFuncionario(contexto.pessoaId()) : null);
        veiculo.setLiberadoEm(novoValor ? LocalDateTime.now() : null);
        veiculoRepository.save(veiculo);

        return buscarDetalhe(contexto, eventoId);
    }

    @Transactional
    public EventoResponse liberarPessoa(ContextoAutenticado contexto, Integer eventoId, Integer pessoaId) {
        Evento evento = buscarEvento(eventoId);
        Autorizacao.exigirPorteiroOuPerfilCompleto(contexto, evento.getCondominio().getId());
        EventoPessoa pessoa = buscarPessoaDoEvento(eventoId, pessoaId);

        boolean novoValor = !pessoa.isLiberado();
        pessoa.setLiberado(novoValor);
        pessoa.setFuncionarioLiberou(novoValor ? buscarFuncionario(contexto.pessoaId()) : null);
        pessoa.setLiberadoEm(novoValor ? LocalDateTime.now() : null);
        pessoaRepository.save(pessoa);

        return buscarDetalhe(contexto, eventoId);
    }

    /**
     * Foto opcional da pessoa (pedido do Romulo: registro de segurança - "em caso de
     * mal-feito, dá pra identificar quem entrou") - ação independente de liberar, sempre
     * restrita a porteiro/perfil completo (nunca o morador, mesmo dono do evento). Se já
     * tinha foto, substitui - remove o objeto velho do bucket antes de subir o novo (mesma
     * ordem segura de {@code DemandaDocumentoService.upload}: só grava na entidade depois
     * do upload dar certo).
     */
    @Transactional
    public EventoResponse enviarFotoPessoa(ContextoAutenticado contexto, Integer eventoId, Integer pessoaId, MultipartFile arquivo) {
        Evento evento = buscarEvento(eventoId);
        Autorizacao.exigirPorteiroOuPerfilCompleto(contexto, evento.getCondominio().getId());
        EventoPessoa pessoa = buscarPessoaDoEvento(eventoId, pessoaId);

        if (arquivo.isEmpty()) {
            throw new InvalidRequestException("Arquivo vazio");
        }
        String tipoMime = arquivo.getContentType();
        if (tipoMime == null || !TIPOS_IMAGEM_PERMITIDOS.contains(tipoMime)) {
            throw new InvalidRequestException("Só imagem (jpeg, png ou webp) é aceita aqui - recebido: " + tipoMime);
        }
        int tamanhoMaximoMb = parametroService.getInt("tamanhoMaximoFotoMb", FOTO_TAMANHO_MAXIMO_MB_PADRAO);
        if (arquivo.getSize() > tamanhoMaximoMb * 1024L * 1024L) {
            throw new InvalidRequestException("Imagem não pode passar de " + tamanhoMaximoMb + "MB");
        }

        String chaveAntiga = pessoa.getFotoChave();
        String chaveNova = "eventos/%d/pessoas/%d/%s%s".formatted(eventoId, pessoaId, UUID.randomUUID(), extensaoPara(tipoMime));
        try (var entrada = arquivo.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(chaveNova)
                    .stream(entrada, arquivo.getSize(), -1)
                    .contentType(tipoMime)
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao enviar a foto pro storage", ex);
        }
        if (chaveAntiga != null) {
            removerObjetoStorage(chaveAntiga);
        }

        pessoa.setFotoChave(chaveNova);
        pessoa.setFotoTipoMime(tipoMime);
        pessoa.setFotoTamanhoBytes((int) arquivo.getSize());
        pessoa.setFuncionarioFoto(buscarFuncionario(contexto.pessoaId()));
        pessoa.setFotoEm(LocalDateTime.now());
        pessoaRepository.save(pessoa);

        return buscarDetalhe(contexto, eventoId);
    }

    /** Remoção física de verdade (não é um registro de auditoria, só um arquivo). */
    @Transactional
    public EventoResponse removerFotoPessoa(ContextoAutenticado contexto, Integer eventoId, Integer pessoaId) {
        Evento evento = buscarEvento(eventoId);
        Autorizacao.exigirPorteiroOuPerfilCompleto(contexto, evento.getCondominio().getId());
        EventoPessoa pessoa = buscarPessoaDoEvento(eventoId, pessoaId);

        if (pessoa.getFotoChave() != null) {
            removerObjetoStorage(pessoa.getFotoChave());
        }
        pessoa.setFotoChave(null);
        pessoa.setFotoTipoMime(null);
        pessoa.setFotoTamanhoBytes(null);
        pessoa.setFuncionarioFoto(null);
        pessoa.setFotoEm(null);
        pessoaRepository.save(pessoa);

        return buscarDetalhe(contexto, eventoId);
    }

    /** Usado por {@code EventoController.baixarFotoPessoa} pra montar a resposta via
     * {@code ArquivoStorageService.baixar(...)} - mesma checagem de autorização de
     * {@link #enviarFotoPessoa}/{@link #removerFotoPessoa}. */
    public EventoPessoa buscarFotoPessoa(ContextoAutenticado contexto, Integer eventoId, Integer pessoaId) {
        Evento evento = buscarEvento(eventoId);
        Autorizacao.exigirPorteiroOuPerfilCompleto(contexto, evento.getCondominio().getId());
        EventoPessoa pessoa = buscarPessoaDoEvento(eventoId, pessoaId);
        if (pessoa.getFotoChave() == null) {
            throw new ResourceNotFoundException("Essa pessoa ainda não tem foto");
        }
        return pessoa;
    }

    /** "Visitante recorrente" (pedido do Romulo: "pra que o porteiro não precise tirar a
     * foto de novo, só conferir") - nome+documento mais recentes do morador QUE JÁ TÊM
     * foto, deduplicados (mesmo critério de {@link #listarCandidatosPessoas}, mas aqui
     * guardando a {@link EventoPessoa} inteira, não um DTO, porque precisamos da
     * {@code fotoChave} de origem pra copiar). */
    private Map<String, EventoPessoa> mapaFotosRecentes(Integer moradorId) {
        Map<String, EventoPessoa> mapa = new HashMap<>();
        for (EventoPessoa pessoa : pessoaRepository.findTop100ByEvento_Morador_IdOrderByIdDesc(moradorId)) {
            if (pessoa.getFotoChave() == null) {
                continue;
            }
            String chave = pessoa.getNome().toLowerCase() + "|" + pessoa.getDocumento().toLowerCase();
            mapa.putIfAbsent(chave, pessoa);
        }
        return mapa;
    }

    /** Copia (server-side, sem passar bytes pelo nosso backend) a foto de um evento
     * anterior do morador pra dentro de {@code novaPessoa}, se o nome+documento baterem
     * com alguma entrada de {@code fotosRecentes} - silencioso quando não bate (a maioria
     * dos casos: pessoa realmente nova, sem histórico). Preserva {@code fotoEm} da
     * ORIGEM (não "agora") - fica claro, se precisar auditar depois, que é uma foto
     * reaproveitada, não tirada nesse momento. {@code funcionarioFoto} fica nulo: nenhum
     * funcionário agiu nesta pessoa especificamente - ver {@code EventoPessoaResponse}. */
    private void copiarFotoHistorica(EventoPessoa novaPessoa, Map<String, EventoPessoa> fotosRecentes) {
        String chave = novaPessoa.getNome().toLowerCase() + "|" + novaPessoa.getDocumento().toLowerCase();
        EventoPessoa origem = fotosRecentes.get(chave);
        if (origem == null) {
            return;
        }
        String chaveNova = "eventos/%d/pessoas/%d/%s%s".formatted(
                novaPessoa.getEvento().getId(), novaPessoa.getId(), UUID.randomUUID(), extensaoPara(origem.getFotoTipoMime()));
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .object(chaveNova)
                    .source(CopySource.builder().bucket(bucket).object(origem.getFotoChave()).build())
                    .build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao reaproveitar a foto anterior", ex);
        }
        novaPessoa.setFotoChave(chaveNova);
        novaPessoa.setFotoTipoMime(origem.getFotoTipoMime());
        novaPessoa.setFotoTamanhoBytes(origem.getFotoTamanhoBytes());
        novaPessoa.setFotoEm(origem.getFotoEm());
        pessoaRepository.save(novaPessoa);
    }

    private void removerObjetoStorage(String chave) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(chave).build());
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao remover a foto do storage", ex);
        }
    }

    private String extensaoPara(String tipoMime) {
        return switch (tipoMime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private EventoPessoa buscarPessoaDoEvento(Integer eventoId, Integer pessoaId) {
        EventoPessoa pessoa = pessoaRepository
                .findById(pessoaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada: " + pessoaId));
        if (!pessoa.getEvento().getId().equals(eventoId)) {
            throw new ResourceNotFoundException("Pessoa não encontrada nesse evento: " + pessoaId);
        }
        return pessoa;
    }

    private Specification<Evento> especificacao(Integer condominioId, LocalDate dataInicio, LocalDate dataFim, Integer espacoComumId) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("condominio").get("id"), condominioId));
            if (dataInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("data"), dataInicio));
            }
            if (dataFim != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("data"), dataFim));
            }
            if (espacoComumId != null) {
                predicados.add(cb.equal(root.get("espacoComum").get("id"), espacoComumId));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    /** Carrega veículos/pessoas/unidade de todos os eventos da página em 3 queries (não uma
     * por evento) e monta cada {@code EventoResponse} - mesmo espírito de
     * {@code RondaService.contarDemandasPorRonda}. */
    private List<EventoResponse> montarRespostasEmLote(List<Evento> eventos) {
        if (eventos.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = eventos.stream().map(Evento::getId).toList();
        Integer condominioId = eventos.get(0).getCondominio().getId();

        Map<Integer, List<EventoVeiculo>> veiculosPorEvento = new HashMap<>();
        for (EventoVeiculo v : veiculoRepository.findByEventoIdInOrderById(ids)) {
            veiculosPorEvento.computeIfAbsent(v.getEvento().getId(), k -> new ArrayList<>()).add(v);
        }
        Map<Integer, List<EventoPessoa>> pessoasPorEvento = new HashMap<>();
        for (EventoPessoa p : pessoaRepository.findByEventoIdInOrderById(ids)) {
            pessoasPorEvento.computeIfAbsent(p.getEvento().getId(), k -> new ArrayList<>()).add(p);
        }
        List<Integer> moradorIds = eventos.stream().map(e -> e.getMorador().getId()).distinct().toList();
        Map<Integer, String> unidadePorMorador = new HashMap<>();
        for (var vinculo : moradorCondominioRepository.findByCondominioIdAndMoradorIdIn(condominioId, moradorIds)) {
            unidadePorMorador.put(vinculo.getMorador().getId(), vinculo.getNumeroUnidade());
        }

        // Só porteiro/perfil completo chega em listarPagina (ver Autorizacao.
        // exigirPorteiroOuPerfilCompleto acima) - sempre pode ver a foto.
        return eventos.stream()
                .map(evento -> EventoResponse.from(
                        evento,
                        unidadePorMorador.get(evento.getMorador().getId()),
                        veiculosPorEvento.getOrDefault(evento.getId(), List.of()),
                        pessoasPorEvento.getOrDefault(evento.getId(), List.of()),
                        true))
                .toList();
    }

    /** Nulo = própria unidade do morador. Não nulo: precisa existir, pertencer ao mesmo
     * condomínio e estar ativo - reusado por {@link #criar} e {@link #atualizar}. */
    private EspacoComum resolverEspacoComum(Integer espacoComumId, Integer condominioId) {
        if (espacoComumId == null) {
            return null;
        }
        EspacoComum espaco = espacoComumRepository
                .findById(espacoComumId)
                .orElseThrow(() -> new ResourceNotFoundException("Espaço não encontrado: " + espacoComumId));
        if (!espaco.getCondominio().getId().equals(condominioId)) {
            throw new InvalidRequestException("Esse espaço não pertence ao seu condomínio");
        }
        if (espaco.getSituacao() != Situacao.ativo) {
            throw new InvalidRequestException("Esse espaço não está mais disponível");
        }
        return espaco;
    }

    private String buscarUnidade(Integer moradorId, Integer condominioId) {
        return moradorCondominioRepository
                .findByMoradorIdAndCondominioId(moradorId, condominioId)
                .map(vinculo -> vinculo.getNumeroUnidade())
                .orElse(null);
    }

    /** Compartilhado por {@link #atualizar} e {@link #excluir}: só o próprio morador dono
     * do evento, e só enquanto a data do evento ainda não passou. {@code acao} entra na
     * mensagem de erro. */
    private void exigirDonoDoEventoFuturo(ContextoAutenticado contexto, Evento evento, String acao) {
        if (!evento.getMorador().getId().equals(contexto.pessoaId())) {
            throw new ForbiddenException("Só o próprio morador que cadastrou pode " + acao + " nesse evento");
        }
        if (evento.getData().isBefore(LocalDate.now())) {
            throw new ConflictException("Esse evento já aconteceu - não é possível " + acao);
        }
    }

    /** Além de {@link #exigirDonoDoEventoFuturo}, exige também que nenhum veículo/pessoa já
     * tenha sido liberado pela portaria - só usado por {@link #excluir} (apagar o evento
     * INTEIRO depois disso destruiria um registro de liberação já feito). {@link
     * #atualizar} não passa mais por aqui - ele sincroniza item a item, preservando quem já
     * foi liberado (ver {@link #sincronizarPessoas}/{@link #sincronizarVeiculos}), então o
     * evento inteiro continua editável mesmo com item liberado. */
    private void exigirDonoEditavel(ContextoAutenticado contexto, Evento evento, String acao) {
        exigirDonoDoEventoFuturo(contexto, evento, acao);
        List<EventoVeiculo> veiculos = veiculoRepository.findByEventoIdOrderById(evento.getId());
        List<EventoPessoa> pessoas = pessoaRepository.findByEventoIdOrderById(evento.getId());
        boolean temItemLiberado =
                veiculos.stream().anyMatch(EventoVeiculo::isLiberado) || pessoas.stream().anyMatch(EventoPessoa::isLiberado);
        if (temItemLiberado) {
            throw new ConflictException("Já tem pessoa ou veículo liberado pela portaria - não é possível " + acao + " mais");
        }
    }

    private void exigirMorador(ContextoAutenticado contexto) {
        if (!TipoPessoa.morador.name().equals(contexto.tipoPapel())) {
            throw new ForbiddenException("Só morador pode fazer isso");
        }
    }

    /** Autocomplete de "visitante recorrente" (pedido do Romulo) - pessoas que o PRÓPRIO
     * morador logado já cadastrou em qualquer evento anterior dele (nunca de outro
     * morador), mais recente primeiro, deduplicadas por nome+documento. Não recebe
     * eventoId: a busca nasce do morador logado, não de um evento específico - filtrar o
     * que já está na tela do formulário atual é responsabilidade do frontend. */
    public List<EventoPessoaCandidatoResponse> listarCandidatosPessoas(ContextoAutenticado contexto) {
        exigirMorador(contexto);
        Set<String> vistos = new HashSet<>();
        List<EventoPessoaCandidatoResponse> candidatos = new ArrayList<>();
        for (EventoPessoa pessoa : pessoaRepository.findTop100ByEvento_Morador_IdOrderByIdDesc(contexto.pessoaId())) {
            String chave = pessoa.getNome().toLowerCase() + "|" + pessoa.getDocumento().toLowerCase();
            if (vistos.add(chave)) {
                candidatos.add(new EventoPessoaCandidatoResponse(pessoa.getNome(), pessoa.getDocumento()));
                if (candidatos.size() >= 20) {
                    break;
                }
            }
        }
        return candidatos;
    }

    /** Mesmo espírito de {@link #listarCandidatosPessoas}, pra veículo (dedupe por placa). */
    public List<EventoVeiculoCandidatoResponse> listarCandidatosVeiculos(ContextoAutenticado contexto) {
        exigirMorador(contexto);
        Set<String> vistos = new HashSet<>();
        List<EventoVeiculoCandidatoResponse> candidatos = new ArrayList<>();
        for (EventoVeiculo veiculo : veiculoRepository.findTop100ByEvento_Morador_IdOrderByIdDesc(contexto.pessoaId())) {
            if (vistos.add(veiculo.getPlaca().toUpperCase())) {
                candidatos.add(new EventoVeiculoCandidatoResponse(veiculo.getPlaca()));
                if (candidatos.size() >= 20) {
                    break;
                }
            }
        }
        return candidatos;
    }

    private Evento buscarEvento(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + id));
    }

    private Funcionario buscarFuncionario(Integer id) {
        return funcionarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }
}
