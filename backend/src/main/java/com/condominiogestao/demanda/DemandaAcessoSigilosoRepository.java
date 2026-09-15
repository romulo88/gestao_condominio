package com.condominiogestao.demanda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandaAcessoSigilosoRepository extends JpaRepository<DemandaAcessoSigiloso, Integer> {

    List<DemandaAcessoSigiloso> findByDemandaId(Integer demandaId);

    /** Bulk pra várias demandas de uma vez - usado em {@code DemandaService.listar} pra
     * checar quem tem acesso extra a cada demanda sigilosa sem uma consulta por linha. */
    @Query("select a from DemandaAcessoSigiloso a where a.demanda.id in :demandaIds")
    List<DemandaAcessoSigiloso> findByDemandaIdIn(@Param("demandaIds") List<Integer> demandaIds);

    boolean existsByDemandaIdAndMoradorId(Integer demandaId, Integer moradorId);

    boolean existsByDemandaIdAndFuncionarioId(Integer demandaId, Integer funcionarioId);
}
