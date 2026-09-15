package com.condominiogestao.parametro;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.parametro.dto.ParametroCreateRequest;
import com.condominiogestao.parametro.dto.ParametroResponse;
import com.condominiogestao.parametro.dto.ParametroUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Parâmetros gerais do sistema (pedido do Romulo): valor de regra de negócio (ex: máximo
 * de fotos por demanda) editável pelo administrador sem precisar de deploy - o CRUD
 * abaixo é 100% administrador-only ({@link Autorizacao#exigirAdministrador}), ninguém
 * mais vê ou gerencia essa tabela.
 *
 * <p>Quem usa um parâmetro (ex: {@code DemandaDocumentoService}) chama {@link #getInt} em
 * vez de ler o banco direto - o valor fica em cache em memória (recarregado a cada
 * criar/editar/excluir aqui, ver {@link #recarregarCacheAposCommit}) porque parâmetro é
 * lido com frequência (ex: todo upload de anexo) e muda raramente, então não vale a pena
 * bater no banco a cada leitura. Backend com uma instância só (ver README) - se um dia
 * rodar mais de uma instância, isso precisa virar um cache com invalidação distribuída.
 */
@Service
public class ParametroService {

    private final ParametroRepository repository;

    private volatile Map<String, String> cache = Map.of();

    public ParametroService(ParametroRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void carregarCache() {
        recarregarCache();
    }

    private void recarregarCache() {
        cache = repository.findAll().stream()
                .collect(Collectors.toMap(Parametro::getNome, Parametro::getValor));
    }

    /** Recarrega o cache só DEPOIS do commit da transação que alterou a tabela.
     *
     * <p>Chamar {@link #recarregarCache} direto de dentro de um método {@code @Transactional}
     * enxerga o valor novo (o {@code findAll} força o flush), mas ele ainda não está
     * commitado - se o commit falhar depois disso (ex: dois administradores criando o mesmo
     * {@code nome} ao mesmo tempo, um deles estoura o UNIQUE no commit), o cache fica com um
     * valor que nunca foi persistido e só se corrige na próxima escrita ou no restart.
     *
     * <p>Fora de transação (ex: {@link #carregarCache} no {@code @PostConstruct}) recarrega
     * na hora, comportamento idêntico ao de antes. */
    private void recarregarCacheAposCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            recarregarCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                recarregarCache();
            }
        });
    }

    /** Lê um parâmetro numérico - {@code valorPadrao} cobre tanto "parâmetro não existe"
     * quanto "valor cadastrado não é um número válido" (uma edição manual errada no banco
     * não pode derrubar a funcionalidade que depende dele, só faz ela seguir com o
     * default). */
    public int getInt(String nome, int valorPadrao) {
        String valor = cache.get(nome);
        if (valor == null) {
            return valorPadrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return valorPadrao;
        }
    }

    public List<ParametroResponse> listar(ContextoAutenticado contexto) {
        Autorizacao.exigirAdministrador(contexto);
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
                .map(ParametroResponse::from)
                .toList();
    }

    @Transactional
    public ParametroResponse criar(ContextoAutenticado contexto, ParametroCreateRequest request) {
        Autorizacao.exigirAdministrador(contexto);
        if (repository.existsByNome(request.nome())) {
            throw new ConflictException("Já existe um parâmetro \"" + request.nome() + "\"");
        }

        Parametro parametro = new Parametro();
        parametro.setNome(request.nome());
        parametro.setDescricao(request.descricao());
        parametro.setValor(request.valor());

        Parametro salvo = repository.save(parametro);
        recarregarCacheAposCommit();
        return ParametroResponse.from(salvo);
    }

    /** Só corrige descrição/valor - {@code nome} é fixo (ver {@link Parametro}). */
    @Transactional
    public ParametroResponse atualizar(ContextoAutenticado contexto, Integer id, ParametroUpdateRequest request) {
        Autorizacao.exigirAdministrador(contexto);
        Parametro parametro = buscarEntidadePorId(id);

        parametro.setDescricao(request.descricao());
        parametro.setValor(request.valor());

        Parametro salvo = repository.save(parametro);
        recarregarCacheAposCommit();
        return ParametroResponse.from(salvo);
    }

    @Transactional
    public void excluir(ContextoAutenticado contexto, Integer id) {
        Autorizacao.exigirAdministrador(contexto);
        Parametro parametro = buscarEntidadePorId(id);
        repository.delete(parametro);
        recarregarCacheAposCommit();
    }

    private Parametro buscarEntidadePorId(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parâmetro não encontrado: " + id));
    }
}
