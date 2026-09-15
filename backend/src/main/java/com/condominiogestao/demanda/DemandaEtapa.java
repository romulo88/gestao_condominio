package com.condominiogestao.demanda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subtarefas internas da demanda, com prazo próprio (item 4.5). Diferente do
 * {@code status_kanban} da demanda - uma demanda tem várias etapas E um status Kanban
 * ao mesmo tempo.
 */
@Entity
@Table(name = "demanda_etapas")
@Getter
@Setter
@NoArgsConstructor
public class DemandaEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etapa")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @Column(columnDefinition = "TEXT")
    private String nome;

    /** Só data (sem hora) e opcional - nem toda etapa precisa de prazo marcado. */
    private LocalDate prazo;

    private boolean concluida = false;

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    private Integer ordem = 0;
}
