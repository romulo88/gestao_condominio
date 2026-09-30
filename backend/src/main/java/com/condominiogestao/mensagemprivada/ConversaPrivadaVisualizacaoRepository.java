package com.condominiogestao.mensagemprivada;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversaPrivadaVisualizacaoRepository extends JpaRepository<ConversaPrivadaVisualizacao, Integer> {

    List<ConversaPrivadaVisualizacao> findByConversaIdOrderByVisualizadoEmDesc(Integer conversaId);
}
