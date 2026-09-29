package com.condominiogestao.espacocomum;

import com.condominiogestao.common.Situacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspacoComumRepository extends JpaRepository<EspacoComum, Integer> {

    List<EspacoComum> findByCondominioId(Integer condominioId);

    /** Só os ativos - opção de "local" ao cadastrar um Evento. */
    List<EspacoComum> findByCondominioIdAndSituacao(Integer condominioId, Situacao situacao);

    boolean existsByCondominioIdAndNome(Integer condominioId, String nome);

    /** Mesma checagem, excluindo o próprio espaço - usada na edição (renomear pro
     * próprio nome que já tem não é conflito com "outro" espaço). */
    boolean existsByCondominioIdAndNomeAndIdNot(Integer condominioId, String nome, Integer id);
}
