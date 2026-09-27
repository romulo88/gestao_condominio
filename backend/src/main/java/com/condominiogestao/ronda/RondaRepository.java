package com.condominiogestao.ronda;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** {@link JpaSpecificationExecutor} (Specification/Criteria, não `@Query` com string JPQL)
 * pra tela "Rondas" do síndico - filtros por rondista/período são todos opcionais, e um
 * `@Query` com padrão "{@code :param IS NULL OR ...}" faz o Postgres tentar inferir o tipo
 * de um parâmetro só a partir de um `IS NULL` isolado quando o valor é de fato nulo, o que
 * falha pra `Integer`/`LocalDateTime` (`PSQLException: could not determine data type of
 * parameter`) mesmo com CAST explícito no JPQL (achado testando ao vivo). Com Specification,
 * o predicado de um filtro ausente simplesmente não entra na query - nunca chega a existir
 * um parâmetro nulo sem tipo pro Postgres se confundir (ver {@code RondaService.especificacao}). */
public interface RondaRepository extends JpaRepository<Ronda, Integer>, JpaSpecificationExecutor<Ronda> {

    /** "Só uma ronda ativa por vez" é por rondista, nunca por condomínio - vários
     * rondistas do mesmo condomínio podem ter, cada um, sua própria ronda em andamento ao
     * mesmo tempo (pedido do Romulo). */
    Optional<Ronda> findByFuncionarioIdAndStatus(Integer funcionarioId, RondaStatus status);
}
