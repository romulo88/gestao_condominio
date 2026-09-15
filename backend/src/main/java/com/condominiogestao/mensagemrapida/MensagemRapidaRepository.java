package com.condominiogestao.mensagemrapida;

import com.condominiogestao.common.Situacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemRapidaRepository extends JpaRepository<MensagemRapida, Integer> {

    /** Só as ativas - o que aparece na listagem/opções pra usar. */
    List<MensagemRapida> findByCondominioIdAndSituacaoOrderByCreatedAtDesc(Integer condominioId, Situacao situacao);
}
