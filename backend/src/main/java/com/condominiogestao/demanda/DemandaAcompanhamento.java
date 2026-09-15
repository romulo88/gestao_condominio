package com.condominiogestao.demanda;

import com.condominiogestao.morador.Morador;
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
 * Funcionalidade "Acompanhar" (pedido do Romulo): morador marca um check numa demanda
 * que ele não abriu, pra receber as mudanças de status dela (aprovação/recusa,
 * movimentação de coluna no Kanban) junto do alerta de login das próprias demandas -
 * ver {@link DemandaService#mudancasStatus}. Uma linha por par (demanda, morador) -
 * {@code uq_demanda_acompanhamento} no banco garante isso, além do service conferir
 * antes de inserir de novo (idempotente).
 */
@Entity
@Table(name = "demanda_acompanhamentos")
@Getter
@Setter
@NoArgsConstructor
public class DemandaAcompanhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acompanhamento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador", nullable = false)
    private Morador morador;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
