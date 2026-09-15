package com.condominiogestao.demanda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandaEtiquetaRepository extends JpaRepository<DemandaEtiqueta, DemandaEtiquetaId> {

    List<DemandaEtiqueta> findByDemandaId(Integer demandaId);

    /** Com a etiqueta já carregada (join fetch) - evita N+1 ao montar o card no Kanban. */
    @Query("select de from DemandaEtiqueta de join fetch de.etiqueta where de.demanda.id = :demandaId order by de.createdAt")
    List<DemandaEtiqueta> findByDemandaIdComEtiqueta(@Param("demandaId") Integer demandaId);

    /** Mesma ideia, mas pra várias demandas de uma vez - usado em {@code listar()} (uma
     * consulta só pra todas as demandas do condomínio, em vez de uma por demanda). */
    @Query("select de from DemandaEtiqueta de join fetch de.etiqueta where de.demanda.id in :demandaIds order by de.createdAt")
    List<DemandaEtiqueta> findByDemandaIdInComEtiqueta(@Param("demandaIds") List<Integer> demandaIds);
}
