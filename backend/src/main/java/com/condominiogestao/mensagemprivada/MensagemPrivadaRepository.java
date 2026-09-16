package com.condominiogestao.mensagemprivada;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemPrivadaRepository extends JpaRepository<MensagemPrivada, Integer> {

    /** Ordem cronológica - o frontend renderiza a conversa como chat (mais antiga primeiro). */
    List<MensagemPrivada> findByConversaIdOrderByCreatedAtAsc(Integer conversaId);

    /** Todas as mensagens de várias conversas de uma vez - usada pelo cálculo em lote de
     * pendência (ícone com destaque vermelho) na listagem, sem N+1. Filtragem por "é de
     * outro participante" e "depois da última visualização" é feita em Java (mesmo padrão
     * de {@code DemandaResponsavelService}), não em JPQL - volume por conversa é pequeno. */
    List<MensagemPrivada> findByConversaIdIn(List<Integer> conversaIds);
}
