package com.condominiogestao.demanda;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chave composta de {@link DemandaEtiqueta} - junção pura, sem PK própria. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DemandaEtiquetaId implements Serializable {

    private Integer idDemanda;
    private Integer idEtiqueta;

    public DemandaEtiquetaId(Integer idDemanda, Integer idEtiqueta) {
        this.idDemanda = idDemanda;
        this.idEtiqueta = idEtiqueta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DemandaEtiquetaId that)) return false;
        return Objects.equals(idDemanda, that.idDemanda) && Objects.equals(idEtiqueta, that.idEtiqueta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDemanda, idEtiqueta);
    }
}
