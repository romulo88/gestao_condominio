package com.condominiogestao.morador;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MoradorCondominioRepository extends JpaRepository<MoradorCondominio, Integer> {

    List<MoradorCondominio> findByMoradorId(Integer moradorId);

    List<MoradorCondominio> findByCondominioId(Integer condominioId);

    boolean existsByMoradorIdAndCondominioId(Integer moradorId, Integer condominioId);

    /** "Ativo" aqui é os dois níveis ao mesmo tempo: o vínculo com esse condomínio E a
     * situação GLOBAL do morador - mesmo critério de FuncionarioCondominioRepository. */
    @Query("SELECT COUNT(mc) FROM MoradorCondominio mc WHERE mc.condominio.id = :condominioId "
            + "AND mc.situacao = com.condominiogestao.common.Situacao.ativo "
            + "AND mc.morador.situacao = com.condominiogestao.common.Situacao.ativo")
    long countAtivosPorCondominio(@Param("condominioId") Integer condominioId);

    /** Página da listagem do cadastro de condomínio - mesmo espírito e mesma convenção de
     * {@code FuncionarioCondominioRepository.buscarPorCondominio} (pedido do Romulo:
     * paginação de 15 registros, "a ideia é performance para não listar todos de vez").
     * Busca só por nome (CPF saiu do sistema, v177/LGPD). */
    @Query("SELECT mc FROM MoradorCondominio mc "
            + "WHERE mc.condominio.id = :condominioId "
            + "AND (:buscaNome IS NULL "
            + "     OR LOWER(mc.morador.pessoa.nome) LIKE :buscaNome) "
            + "ORDER BY mc.morador.pessoa.nome ASC")
    Page<MoradorCondominio> buscarPorCondominio(
            @Param("condominioId") Integer condominioId,
            @Param("buscaNome") String buscaNome,
            Pageable pageable);
}
