package com.condominiogestao.demanda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandaResponsavelRepository extends JpaRepository<DemandaResponsavel, Integer> {

    List<DemandaResponsavel> findByDemandaId(Integer demandaId);

    /** Em lote pra listagem (avatar de responsável no card do Kanban) - evita N+1. */
    List<DemandaResponsavel> findByDemandaIdIn(List<Integer> demandaIds);

    boolean existsByDemandaIdAndFuncionarioId(Integer demandaId, Integer funcionarioId);

    /** Todas as atribuições de um funcionário - usado pra visibilidade de perfil restrito
     * (`DemandaService.listar`/`listarPagina`) e pro filtro "Minhas demandas". */
    List<DemandaResponsavel> findByFuncionarioId(Integer funcionarioId);
}
