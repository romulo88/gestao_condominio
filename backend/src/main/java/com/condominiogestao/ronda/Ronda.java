package com.condominiogestao.ronda;

import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.funcionario.Funcionario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Uma ronda "livre" de um rondista (sem pontos de controle nem rota cadastrada - o
 * síndico combina o percurso diretamente com ele). {@code distanciaMetros} só é calculada
 * (Haversine sobre {@link RondaPonto}) na hora de encerrar - qualquer um dos três jeitos de
 * encerrar (ver {@link RondaStatus}).
 */
@Entity
@Table(name = "rondas")
@Getter
@Setter
@NoArgsConstructor
public class Ronda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ronda")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(name = "iniciada_em", nullable = false)
    private LocalDateTime iniciadaEm;

    @Column(name = "finalizada_em")
    private LocalDateTime finalizadaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RondaStatus status = RondaStatus.em_andamento;

    @Column(name = "distancia_metros")
    private Double distanciaMetros;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
