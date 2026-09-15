package com.condominiogestao.demanda;

import com.condominiogestao.common.Autorizacao;
import com.condominiogestao.common.ConflictException;
import com.condominiogestao.common.InvalidRequestException;
import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.demanda.dto.DemandaEtiquetaVincularRequest;
import com.condominiogestao.etiqueta.Etiqueta;
import com.condominiogestao.etiqueta.EtiquetaRepository;
import com.condominiogestao.etiqueta.dto.EtiquetaResponse;
import com.condominiogestao.security.ContextoAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vínculo N:N entre demanda e etiqueta (item 4.6) - só funcionário do condomínio da
 * demanda anexa/remove, mesmo critério de {@link DemandaEtapaService} (ferramenta
 * operacional interna, morador não vê etiqueta nenhuma).
 */
@Service
@Transactional(readOnly = true)
public class DemandaEtiquetaService {

    private final DemandaEtiquetaRepository repository;
    private final DemandaRepository demandaRepository;
    private final EtiquetaRepository etiquetaRepository;

    public DemandaEtiquetaService(
            DemandaEtiquetaRepository repository, DemandaRepository demandaRepository, EtiquetaRepository etiquetaRepository) {
        this.repository = repository;
        this.demandaRepository = demandaRepository;
        this.etiquetaRepository = etiquetaRepository;
    }

    @Transactional
    public EtiquetaResponse vincular(ContextoAutenticado contexto, DemandaEtiquetaVincularRequest request) {
        Demanda demanda = buscarDemanda(request.demandaId());
        Autorizacao.exigirFuncionarioDoCondominio(contexto, demanda.getCondominio().getId());

        Etiqueta etiqueta = etiquetaRepository
                .findById(request.etiquetaId())
                .orElseThrow(() -> new ResourceNotFoundException("Etiqueta não encontrada: " + request.etiquetaId()));
        if (!etiqueta.getCondominio().getId().equals(demanda.getCondominio().getId())) {
            throw new InvalidRequestException("Essa etiqueta não pertence a este condomínio");
        }

        DemandaEtiquetaId id = new DemandaEtiquetaId(demanda.getId(), etiqueta.getId());
        if (repository.existsById(id)) {
            throw new ConflictException("Essa etiqueta já está nessa demanda");
        }

        DemandaEtiqueta vinculo = new DemandaEtiqueta();
        vinculo.setDemanda(demanda);
        vinculo.setEtiqueta(etiqueta);
        repository.save(vinculo);

        return EtiquetaResponse.from(etiqueta);
    }

    @Transactional
    public void desvincular(ContextoAutenticado contexto, Integer demandaId, Integer etiquetaId) {
        Demanda demanda = buscarDemanda(demandaId);
        Autorizacao.exigirFuncionarioDoCondominio(contexto, demanda.getCondominio().getId());

        repository.deleteById(new DemandaEtiquetaId(demandaId, etiquetaId));
    }

    private Demanda buscarDemanda(Integer id) {
        return demandaRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demanda não encontrada: " + id));
    }
}
