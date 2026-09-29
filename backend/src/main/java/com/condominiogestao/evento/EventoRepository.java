package com.condominiogestao.evento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** {@link JpaSpecificationExecutor} (Specification/Criteria, não `@Query` com string JPQL)
 * pro calendário da portaria - filtros por período/espaço são opcionais, e um `@Query`
 * com padrão "{@code :param IS NULL OR ...}" faz o Postgres tentar inferir o tipo de um
 * parâmetro só a partir de um `IS NULL` isolado quando o valor é de fato nulo, o que falha
 * pra `Integer`/`LocalDate` (achado documentado em {@code RondaRepository}). Com
 * Specification, o predicado de um filtro ausente simplesmente não entra na query. */
public interface EventoRepository extends JpaRepository<Evento, Integer>, JpaSpecificationExecutor<Evento> {

    /** "Meus eventos" do morador - mais recente primeiro, sem paginação (baixo volume por morador). */
    List<Evento> findByMoradorIdOrderByDataDesc(Integer moradorId);
}
