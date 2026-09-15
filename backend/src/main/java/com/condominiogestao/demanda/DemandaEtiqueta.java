package com.condominiogestao.demanda;

import com.condominiogestao.etiqueta.Etiqueta;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Junção N:N: uma demanda pode ter várias etiquetas, uma etiqueta pode estar em várias
 * demandas.
 */
@Entity
@Table(name = "demanda_etiquetas")
@Getter
@Setter
@NoArgsConstructor
public class DemandaEtiqueta {

    @EmbeddedId
    private DemandaEtiquetaId id = new DemandaEtiquetaId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idDemanda")
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEtiqueta")
    @JoinColumn(name = "id_etiqueta", nullable = false)
    private Etiqueta etiqueta;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
