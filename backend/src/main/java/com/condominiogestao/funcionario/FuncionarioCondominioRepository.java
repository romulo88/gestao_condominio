package com.condominiogestao.funcionario;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FuncionarioCondominioRepository extends JpaRepository<FuncionarioCondominio, Integer> {

    List<FuncionarioCondominio> findByFuncionarioId(Integer funcionarioId);

    List<FuncionarioCondominio> findByCondominioId(Integer condominioId);

    Optional<FuncionarioCondominio> findByFuncionarioIdAndCondominioId(Integer funcionarioId, Integer condominioId);

    boolean existsByFuncionarioIdAndCondominioId(Integer funcionarioId, Integer condominioId);

    /** "Ativo" aqui é os dois níveis ao mesmo tempo: o vínculo com esse condomínio E a
     * situação GLOBAL do funcionário (mesmo critério usado em AuthService.listarContextosAtivos). */
    @Query("SELECT COUNT(fc) FROM FuncionarioCondominio fc WHERE fc.condominio.id = :condominioId "
            + "AND fc.situacao = com.condominiogestao.common.Situacao.ativo "
            + "AND fc.funcionario.situacao = com.condominiogestao.common.Situacao.ativo")
    long countAtivosPorCondominio(@Param("condominioId") Integer condominioId);

    /** Página da listagem do cadastro de condomínio (pedido do Romulo: "criar uma
     * paginação de 15 registros... a ideia é performance para não listar todos de vez").
     * {@code buscaNome}/{@code buscaCpf} vêm nulos quando não há busca (ver
     * {@code FuncionarioCondominioService.listarPaginaPorCondominio}, que monta os dois a
     * partir do MESMO campo de busca da tela - texto vira {@code buscaNome}, os dígitos
     * dele (se tiver algum) viram {@code buscaCpf}) - {@code :buscaNome IS NULL} sozinho
     * já cobre "sem busca" (não precisa repetir a checagem pro CPF). Ordenado por nome -
     * fixo na própria query, não no `Pageable`, pra não depender de o Spring Data
     * conseguir montar sozinho um `ORDER BY` que navegue `funcionario.pessoa.nome` a
     * partir de uma query JPQL escrita à mão. */
    @Query("SELECT fc FROM FuncionarioCondominio fc "
            + "WHERE fc.condominio.id = :condominioId "
            + "AND (:buscaNome IS NULL "
            + "     OR LOWER(fc.funcionario.pessoa.nome) LIKE :buscaNome "
            + "     OR (:buscaCpf IS NOT NULL AND fc.funcionario.pessoa.cpf LIKE :buscaCpf)) "
            + "ORDER BY fc.funcionario.pessoa.nome ASC")
    Page<FuncionarioCondominio> buscarPorCondominio(
            @Param("condominioId") Integer condominioId,
            @Param("buscaNome") String buscaNome,
            @Param("buscaCpf") String buscaCpf,
            Pageable pageable);
}
