package com.condominiogestao.demanda;

import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.kanban.StatusKanban;
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

/**
 * Histórico de mudança de coluna do Kanban: uma linha por transição, com quando e quem
 * mudou. (Aprovação/reprovação já tem seu próprio rastro em {@link Demanda} -
 * dataAprovacao, justificativaReprovacao, funcionarioAprovador - por ser uma transição
 * única, não repetida como o Kanban.)
 */
@Entity
@Table(name = "demanda_status_kanban_historico")
@Getter
@Setter
@NoArgsConstructor
public class DemandaStatusKanbanHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    /** Nullable - a primeira transição de uma demanda não tem "anterior". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_status_anterior")
    private StatusKanban statusAnterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_status_novo", nullable = false)
    private StatusKanban statusNovo;

    /** Quem alterou. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    /** Quando a mudança aconteceu. */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
