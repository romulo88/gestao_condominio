package com.condominiogestao.kanban;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusKanbanRepository extends JpaRepository<StatusKanban, Integer> {

    List<StatusKanban> findByCondominioIdOrderByOrdem(Integer condominioId);

    boolean existsByCondominioIdAndNome(Integer condominioId, String nome);
}
