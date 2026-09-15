package com.condominiogestao.etiqueta;

import com.condominiogestao.common.Situacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtiquetaRepository extends JpaRepository<Etiqueta, Integer> {

    List<Etiqueta> findByCondominioId(Integer condominioId);

    /** Só as ativas - usado pra oferecer como opção pra anexar numa demanda nova. */
    List<Etiqueta> findByCondominioIdAndSituacao(Integer condominioId, Situacao situacao);

    boolean existsByCondominioIdAndDescricao(Integer condominioId, String descricao);

    /** Mesma checagem, excluindo a própria etiqueta - usada na edição (renomear pro
     * próprio nome que já tem não é conflito com "outra" etiqueta). */
    boolean existsByCondominioIdAndDescricaoAndIdNot(Integer condominioId, String descricao, Integer id);
}
