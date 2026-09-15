package com.condominiogestao.etiqueta;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.etiqueta.dto.EtiquetaCreateRequest;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import com.condominiogestao.etiqueta.dto.EtiquetaUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Etiqueta pra classificar demanda (item 4.6) - cadastrada por condomínio, igual
 * {@link com.condominiogestao.kanban.StatusKanban}. Gerenciável (criar/editar) por
 * qualquer funcionário do condomínio (qualquer perfil - tanto pra criar rapidinho no card
 * do Kanban quanto pra pré-cadastrar "padrão" na aba Etiquetas do cadastro de condomínio)
 * OU por administrador em QUALQUER condomínio (aba Etiquetas é usada por ele pra
 * gerenciar condomínio alheio, mesma necessidade do StatusKanban) - nunca por morador.
 * {@link #listarPorCondominio} é a única leitura mais aberta: pedido do Romulo, morador
 * também precisa da lista pra filtrar o Kanban por etiqueta - só recebe as marcadas
 * {@code visivelMorador} (mesmo critério já usado no card/detalhe da demanda).
 */
@Service
@Transactional(readOnly = true)
public class EtiquetaService {

    private final EtiquetaRepository repository;
    private final CondominioRepository condominioRepository;

    public EtiquetaService(EtiquetaRepository repository, CondominioRepository condominioRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
    }

    /** Só as ativas de um condomínio - opções pra anexar numa demanda / listagem da aba /
     * filtro do Kanban. Morador (novo, pedido do Romulo) só recebe as marcadas {@code
     * visivelMorador} - mesmo critério que já esconde etiqueta dele no card/detalhe da
     * demanda; funcionário/administrador continuam vendo todas, ativas ou não filtradas
     * por visibilidade (gerenciam a etiqueta, precisam ver mesmo a que é só interna). */
    public List<EtiquetaResponse> listarPorCondominio(ContextoAutenticado contexto, Integer condominioId) {
        Autorizacao.exigirAdministradorOuFuncionarioOuMoradorDoCondominio(contexto, condominioId);
        List<Etiqueta> etiquetas = repository.findByCondominioIdAndSituacao(condominioId, Situacao.ativo);
        if (Autorizacao.ehMoradorDoCondominio(contexto, condominioId)) {
            etiquetas = etiquetas.stream().filter(Etiqueta::isVisivelMorador).toList();
        }
        return etiquetas.stream().map(EtiquetaResponse::from).toList();
    }

    public EtiquetaResponse buscarPorId(ContextoAutenticado contexto, Integer id) {
        Etiqueta etiqueta = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, etiqueta.getCondominio().getId());
        return EtiquetaResponse.from(etiqueta);
    }

    @Transactional
    public EtiquetaResponse criar(ContextoAutenticado contexto, EtiquetaCreateRequest request) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, request.condominioId());

        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        if (repository.existsByCondominioIdAndDescricao(request.condominioId(), request.descricao())) {
            throw new ConflictException("Já existe uma etiqueta \"" + request.descricao() + "\" nesse condomínio");
        }

        Etiqueta etiqueta = new Etiqueta();
        etiqueta.setCondominio(condominio);
        etiqueta.setDescricao(request.descricao());
        etiqueta.setCor(request.cor());
        etiqueta.setVisivelMorador(request.visivelMorador() == null || request.visivelMorador());

        return EtiquetaResponse.from(repository.save(etiqueta));
    }

    /** Corrige texto/cor/visibilidade pro morador - mesma autorização de {@link #criar}. */
    @Transactional
    public EtiquetaResponse atualizar(ContextoAutenticado contexto, Integer id, EtiquetaUpdateRequest request) {
        Etiqueta etiqueta = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, etiqueta.getCondominio().getId());

        if (!request.descricao().equals(etiqueta.getDescricao())
                && repository.existsByCondominioIdAndDescricaoAndIdNot(
                        etiqueta.getCondominio().getId(), request.descricao(), id)) {
            throw new ConflictException("Já existe uma etiqueta \"" + request.descricao() + "\" nesse condomínio");
        }

        etiqueta.setDescricao(request.descricao());
        etiqueta.setCor(request.cor());
        if (request.visivelMorador() != null) {
            etiqueta.setVisivelMorador(request.visivelMorador());
        }

        return EtiquetaResponse.from(repository.save(etiqueta));
    }

    private Etiqueta buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etiqueta não encontrada: " + id));
    }
}
