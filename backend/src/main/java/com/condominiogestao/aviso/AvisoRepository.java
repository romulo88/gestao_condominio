package com.condominiogestao.aviso;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvisoRepository extends JpaRepository<Aviso, Integer> {

    List<Aviso> findByCondominioIdOrderByCreatedAtDesc(Integer condominioId);

    @Query("SELECT a FROM Aviso a WHERE a.condominio.id = :condominioId "
            + "AND a.situacao = com.condominiogestao.common.Situacao.ativo "
            + "AND (a.dataExpiracao IS NULL OR a.dataExpiracao > :agora) "
            + "ORDER BY a.createdAt DESC")
    List<Aviso> findVisiveisPorCondominio(@Param("condominioId") Integer condominioId, @Param("agora") LocalDateTime agora);
}
