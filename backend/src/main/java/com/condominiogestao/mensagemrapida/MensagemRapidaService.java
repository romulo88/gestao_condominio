package com.condominiogestao.mensagemrapida;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.condominio.CondominioRepository;
import com.condominiogestao.mensagemrapida.dto.MensagemRapidaCreateRequest;
import com.condominiogestao.mensagemrapida.dto.MensagemRapidaResponse;
import com.condominiogestao.mensagemrapida.dto.MensagemRapidaUpdateRequest;
import com.condominiogestao.security.ContextoAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mensagem rápida - cadastrada por condomínio, igual {@link com.condominiogestao.etiqueta.Etiqueta}.
 * Visível/gerenciável por qualquer funcionário do condomínio (qualquer perfil) OU por
 * administrador em QUALQUER condomínio (aba "Mensagens rápidas" do cadastro de condomínio
 * é usada por ele pra gerenciar condomínio alheio, mesma necessidade de Etiqueta/StatusKanban).
 */
@Service
@Transactional(readOnly = true)
public class MensagemRapidaService {

    private final MensagemRapidaRepository repository;
    private final CondominioRepository condominioRepository;

    public MensagemRapidaService(MensagemRapidaRepository repository, CondominioRepository condominioRepository) {
        this.repository = repository;
        this.condominioRepository = condominioRepository;
    }

    /** Só as ativas de um condomínio - o que aparece na listagem da aba. */
    public List<MensagemRapidaResponse> listarPorCondominio(ContextoAutenticado contexto, Integer condominioId) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, condominioId);
        return repository.findByCondominioIdAndSituacaoOrderByCreatedAtDesc(condominioId, Situacao.ativo).stream()
                .map(MensagemRapidaResponse::from)
                .toList();
    }

    @Transactional
    public MensagemRapidaResponse criar(ContextoAutenticado contexto, MensagemRapidaCreateRequest request) {
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, request.condominioId());

        Condominio condominio = condominioRepository
                .findById(request.condominioId())
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + request.condominioId()));

        MensagemRapida mensagem = new MensagemRapida();
        mensagem.setCondominio(condominio);
        mensagem.setTexto(request.texto());
        mensagem.setCarater(request.carater());

        return MensagemRapidaResponse.from(repository.save(mensagem));
    }

    /** Só corrige texto/caráter - mesma autorização de {@link #criar}. */
    @Transactional
    public MensagemRapidaResponse atualizar(ContextoAutenticado contexto, Integer id, MensagemRapidaUpdateRequest request) {
        MensagemRapida mensagem = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, mensagem.getCondominio().getId());

        mensagem.setTexto(request.texto());
        mensagem.setCarater(request.carater());

        return MensagemRapidaResponse.from(repository.save(mensagem));
    }

    /** "Excluir" na aplicação é soft-delete (situacao = inativo) - mesmo espírito de
     * {@code Aviso.desativar}, sem apagar nada do banco. */
    @Transactional
    public MensagemRapidaResponse excluir(ContextoAutenticado contexto, Integer id) {
        MensagemRapida mensagem = buscarEntidadePorId(id);
        Autorizacao.exigirAdministradorOuFuncionarioDoCondominio(contexto, mensagem.getCondominio().getId());

        mensagem.setSituacao(Situacao.inativo);

        return MensagemRapidaResponse.from(repository.save(mensagem));
    }

    private MensagemRapida buscarEntidadePorId(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensagem rápida não encontrada: " + id));
    }
}
