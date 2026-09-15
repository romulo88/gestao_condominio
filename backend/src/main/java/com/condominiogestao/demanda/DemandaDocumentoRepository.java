package com.condominiogestao.demanda;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandaDocumentoRepository extends JpaRepository<DemandaDocumento, Integer> {

    List<DemandaDocumento> findByDemandaId(Integer demandaId);

    /** Pra montar `DemandaResponse.temAnexos` em lote (listagem) sem N+1 - uma consulta só
     * pra todas as demandas carregadas, mesmo padrão de `DemandaAcessoSigilosoRepository`. */
    List<DemandaDocumento> findByDemandaIdIn(List<Integer> demandaIds);

    /** Idem, mas pra uma demanda só (ações pontuais: aprovar/reprovar/mover/alternar
     * sigilo, onde não vale a pena montar um `Set` só pra uma linha). */
    boolean existsByDemandaId(Integer demandaId);

    /** Usado por {@code DemandaDocumentoService.upload} pra aplicar o limite de fotos/vídeo
     * por demanda (pedido do Romulo - hoje um parâmetro configurável, ver
     * {@code ParametroService}) - `prefixoTipoMime` é {@code "image/"} ou {@code "video/"},
     * contando só o que já está de verdade salvo (não conta a tentativa atual, que ainda
     * nem existe). */
    long countByDemandaIdAndTipoMimeStartingWith(Integer demandaId, String prefixoTipoMime);
}
