package com.condominiogestao.ronda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Um ponto de GPS gravado durante a ronda. {@code capturadoEm} é o horário real da
 * leitura no celular (vem do cliente); {@code createdAt} é quando chegou no servidor -
 * podem divergir porque os pontos são enviados em lote a cada ~20s, não um a um. */
@Entity
@Table(name = "ronda_pontos")
@Getter
@Setter
@NoArgsConstructor
public class RondaPonto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ponto")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ronda", nullable = false)
    private Ronda ronda;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "capturado_em", nullable = false)
    private LocalDateTime capturadoEm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
