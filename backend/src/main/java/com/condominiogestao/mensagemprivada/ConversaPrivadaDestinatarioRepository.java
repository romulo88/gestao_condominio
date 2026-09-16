package com.condominiogestao.mensagemprivada;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversaPrivadaDestinatarioRepository extends JpaRepository<ConversaPrivadaDestinatario, Integer> {

    List<ConversaPrivadaDestinatario> findByConversaId(Integer conversaId);

    /** Em lote pra listagem (avatares dos destinatários + cálculo de pendência por conversa) -
     * evita N+1, mesmo padrão de {@code DemandaResponsavelRepository.findByDemandaIdIn}. */
    List<ConversaPrivadaDestinatario> findByConversaIdIn(List<Integer> conversaIds);

    Optional<ConversaPrivadaDestinatario> findByConversaIdAndFuncionarioId(Integer conversaId, Integer funcionarioId);

    boolean existsByConversaIdAndFuncionarioId(Integer conversaId, Integer funcionarioId);
}
