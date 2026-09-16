package com.condominiogestao.mensagemprivada;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversaPrivadaRepository extends JpaRepository<ConversaPrivada, Integer> {

    /** Visão do morador: só as conversas que ele mesmo iniciou - nunca as dos vizinhos
     * (privacidade estrita, pedido explícito do Romulo - nem síndico enxerga por padrão). */
    List<ConversaPrivada> findByCondominioIdAndMoradorAutorIdOrderByUpdatedAtDesc(
            Integer condominioId, Integer moradorAutorId);

    /** Visão do funcionário: conversas que ele iniciou OU pra onde foi endereçado (ver
     * {@link ConversaPrivadaDestinatario}) - um funcionário só enxerga o que participa,
     * mesma regra de privacidade estrita. */
    @Query("SELECT c FROM ConversaPrivada c WHERE c.condominio.id = :condominioId AND ("
            + "c.funcionarioAutor.id = :funcionarioId OR EXISTS ("
            + "  SELECT 1 FROM ConversaPrivadaDestinatario d WHERE d.conversa = c AND d.funcionario.id = :funcionarioId"
            + ")) ORDER BY c.updatedAt DESC")
    List<ConversaPrivada> buscarParaFuncionario(
            @Param("condominioId") Integer condominioId, @Param("funcionarioId") Integer funcionarioId);
}
