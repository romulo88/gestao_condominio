package com.condominiogestao.aviso;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvisoRepository extends JpaRepository<Aviso, Integer> {

    /** Fixado no topo sempre primeiro (pedido do Romulo), dentro disso mais recente primeiro. */
    List<Aviso> findByCondominioIdOrderByFixadoNoTopoDescCreatedAtDesc(Integer condominioId);

    /** Fixado no topo (pedido do Romulo: "por entender que ele é fixo... a data de
     * expiração não importe") ignora completamente a expiração - fica visível enquanto
     * {@code situacao = ativo}, mesmo com {@code dataExpiracao} no passado (ou nem
     * preenchida). Só desativando ele some do mural. */
    @Query("SELECT a FROM Aviso a WHERE a.condominio.id = :condominioId "
            + "AND a.situacao = com.condominiogestao.common.Situacao.ativo "
            + "AND (a.fixadoNoTopo = true OR a.dataExpiracao IS NULL OR a.dataExpiracao > :agora) "
            + "ORDER BY a.fixadoNoTopo DESC, a.createdAt DESC")
    List<Aviso> findVisiveisPorCondominio(@Param("condominioId") Integer condominioId, @Param("agora") LocalDateTime agora);

    /** Usado por {@code AvisoService.fixarNoTopo} pra desfixar o anterior antes de fixar o
     * novo - só 1 por condomínio ao mesmo tempo. */
    Optional<Aviso> findByCondominioIdAndFixadoNoTopoTrue(Integer condominioId);
}
