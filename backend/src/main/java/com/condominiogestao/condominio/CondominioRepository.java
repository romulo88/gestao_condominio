package com.condominiogestao.condominio;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominioRepository extends JpaRepository<Condominio, Integer> {

    Optional<Condominio> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    /** Pra checar duplicidade num update sem acusar o próprio registro sendo editado. */
    boolean existsByCnpjAndIdNot(String cnpj, Integer id);

    /** Resolve o condomínio a partir do token do link público do Kanban ({@code GET
     * /api/kanban-publico/{token}}) - vazio quando o token não existe, foi revogado, ou
     * nunca existiu. */
    Optional<Condominio> findByKanbanPublicoToken(String kanbanPublicoToken);
}
