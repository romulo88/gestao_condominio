package com.condominiogestao.condominio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlocoRepository extends JpaRepository<Bloco, Integer> {

    List<Bloco> findByCondominioId(Integer condominioId);

    boolean existsByCondominioIdAndNome(Integer condominioId, String nome);

    /** Mesma checagem, excluindo o próprio bloco - usada na edição. */
    boolean existsByCondominioIdAndNomeAndIdNot(Integer condominioId, String nome, Integer id);

    long countByCondominioId(Integer condominioId);
}
