package com.condominiogestao.condominio;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.Cnpj;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.dto.CondominioCreateRequest;
import com.condominiogestao.condominio.dto.CondominioResponse;
import com.condominiogestao.condominio.dto.CondominioUpdateRequest;
import com.condominiogestao.funcionario.FuncionarioCondominioRepository;
import com.condominiogestao.morador.MoradorCondominioRepository;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class CondominioService {

    private final CondominioRepository repository;
    private final BlocoRepository blocoRepository;
    private final FuncionarioCondominioRepository funcionarioCondominioRepository;
    private final MoradorCondominioRepository moradorCondominioRepository;
    private final CondominioGifService gifService;

    public CondominioService(
            CondominioRepository repository,
            BlocoRepository blocoRepository,
            FuncionarioCondominioRepository funcionarioCondominioRepository,
            MoradorCondominioRepository moradorCondominioRepository,
            CondominioGifService gifService) {
        this.repository = repository;
        this.blocoRepository = blocoRepository;
        this.funcionarioCondominioRepository = funcionarioCondominioRepository;
        this.moradorCondominioRepository = moradorCondominioRepository;
        this.gifService = gifService;
    }

    /** Administrador vê todos os condomínios; funcionário/morador só vê o condomínio do
     * contexto atual (o que ele escolheu no login) - antes disso não filtrava nada, então
     * qualquer pessoa logada via qualquer papel via a lista inteira do sistema. */
    public List<CondominioResponse> listar(ContextoAutenticado contexto) {
        if (Autorizacao.ehAdministrador(contexto)) {
            return repository.findAll().stream().map(this::paraResponse).toList();
        }
        if (contexto.condominioId() == null) {
            return List.of();
        }
        return repository.findById(contexto.condominioId()).map(c -> List.of(paraResponse(c))).orElse(List.of());
    }

    public CondominioResponse buscarPorId(Integer id) {
        return paraResponse(buscarEntidadePorId(id));
    }

    /** Cadastrar um condomínio novo é só do administrador - síndico/sub-síndico gerenciam
     * o condomínio deles, mas não criam outros do zero. */
    @Transactional
    public CondominioResponse criar(ContextoAutenticado contexto, CondominioCreateRequest request) {
        Autorizacao.exigirAdministrador(contexto);

        String cnpj = Cnpj.normalizar(request.cnpj());
        if (repository.existsByCnpj(cnpj)) {
            throw new ConflictException("Já existe um condomínio cadastrado com o CNPJ " + cnpj);
        }

        Condominio condominio = new Condominio();
        condominio.setNome(request.nome());
        condominio.setCnpj(cnpj);
        condominio.setTipo(request.tipo());
        condominio.setQuantidadeCasas(request.quantidadeCasas());
        // situacao já nasce 'ativo' por default no campo da entidade

        return paraResponse(repository.save(condominio));
    }

    /** Editar é administrador OU síndico/sub-síndico do PRÓPRIO condomínio (ver Autorizacao). */
    @Transactional
    public CondominioResponse atualizar(ContextoAutenticado contexto, Integer id, CondominioUpdateRequest request) {
        Autorizacao.exigirAdministradorOuGestor(contexto, id);

        Condominio condominio = buscarEntidadePorId(id);

        String cnpj = Cnpj.normalizar(request.cnpj());
        if (repository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new ConflictException("Já existe um condomínio cadastrado com o CNPJ " + cnpj);
        }

        condominio.setNome(request.nome());
        condominio.setCnpj(cnpj);
        condominio.setTipo(request.tipo());
        condominio.setQuantidadeCasas(request.quantidadeCasas());

        return paraResponse(repository.save(condominio));
    }

    /**
     * "Remover" um condomínio na prática é desativar (situacao = inativo), não apagar a
     * linha - um DELETE de verdade quebraria a chave estrangeira de tudo que já
     * referencia esse condomínio (blocos, vínculos, avisos, etc.), e esse projeto evita
     * exclusão física em todo o resto do modelo (mesmo padrão de Funcionario/Morador/Aviso).
     * Só administrador - diferente de editar, desativar NÃO é liberado pra síndico/sub-síndico.
     */
    @Transactional
    public CondominioResponse desativar(ContextoAutenticado contexto, Integer id) {
        Autorizacao.exigirAdministrador(contexto);

        Condominio condominio = buscarEntidadePorId(id);
        condominio.setSituacao(Situacao.inativo);

        return paraResponse(repository.save(condominio));
    }

    /** Sobe (ou substitui) o GIF opcional do condomínio - mesma autorização de editar o
     * condomínio (administrador ou síndico/sub-síndico do próprio). */
    @Transactional
    public CondominioResponse atualizarGif(ContextoAutenticado contexto, Integer id, MultipartFile arquivo) {
        Autorizacao.exigirAdministradorOuGestor(contexto, id);
        Condominio condominio = buscarEntidadePorId(id);
        gifService.atualizar(id, arquivo);
        return paraResponse(condominio);
    }

    @Transactional
    public CondominioResponse removerGif(ContextoAutenticado contexto, Integer id) {
        Autorizacao.exigirAdministradorOuGestor(contexto, id);
        Condominio condominio = buscarEntidadePorId(id);
        gifService.remover(id);
        return paraResponse(condominio);
    }

    /** Gera (ou devolve o já existente) o token do link público de leitura do Kanban -
     * pedido do Romulo: "deixar o kanban disponível em um link externo, independente do
     * usuário estar logado". Idempotente de propósito: chamar de novo com um link já
     * gerado devolve o MESMO token, não troca - pra invalidar um link já compartilhado é
     * preciso revogar explicitamente ({@link #revogarLinkPublicoKanban}) e gerar outro
     * depois, não um efeito colateral de reabrir a tela. Mesma autorização de editar o
     * condomínio (administrador, ou síndico/sub-síndico do próprio). */
    @Transactional
    public CondominioResponse gerarLinkPublicoKanban(ContextoAutenticado contexto, Integer id) {
        Autorizacao.exigirAdministradorOuGestor(contexto, id);
        Condominio condominio = buscarEntidadePorId(id);
        if (condominio.getKanbanPublicoToken() == null) {
            // UUID (122 bits de entropia) - inadivinhável por força bruta, e não carrega
            // nenhuma relação sequencial/previsível com o id do condomínio.
            condominio.setKanbanPublicoToken(UUID.randomUUID().toString());
            repository.save(condominio);
        }
        return paraResponse(condominio);
    }

    /** Invalida o link público atual (se existir) - qualquer link já compartilhado para
     * de funcionar (`GET /api/kanban-publico/{token}` passa a dar 404). Pode gerar um
     * link novo depois normalmente, com um token diferente. Mesma autorização de
     * {@link #gerarLinkPublicoKanban}. */
    @Transactional
    public CondominioResponse revogarLinkPublicoKanban(ContextoAutenticado contexto, Integer id) {
        Autorizacao.exigirAdministradorOuGestor(contexto, id);
        Condominio condominio = buscarEntidadePorId(id);
        condominio.setKanbanPublicoToken(null);
        return paraResponse(repository.save(condominio));
    }

    /** Chave bruta do GIF (ver `CondominioGifService.chave`) - usada por `GET
     * /api/condominios/{id}/gif`, que serve a imagem direto em vez de devolver um link
     * assinado do MinIO. */
    public String chaveGif(Integer id) {
        return gifService.chave(id);
    }

    private CondominioResponse paraResponse(Condominio condominio) {
        Integer quantidadeBlocos = condominio.getTipo() == CondominioTipo.apartamento
                ? (int) blocoRepository.countByCondominioId(condominio.getId())
                : null;
        int quantidadeFuncionariosAtivos =
                (int) funcionarioCondominioRepository.countAtivosPorCondominio(condominio.getId());
        int quantidadeMoradoresAtivos = (int) moradorCondominioRepository.countAtivosPorCondominio(condominio.getId());
        String gifUrl = gifService.buscarUrl(condominio.getId());
        return CondominioResponse.from(
                condominio, quantidadeBlocos, quantidadeFuncionariosAtivos, quantidadeMoradoresAtivos, gifUrl);
    }

    private Condominio buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + id));
    }
}
