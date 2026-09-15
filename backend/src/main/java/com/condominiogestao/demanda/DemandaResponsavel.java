package com.condominiogestao.demanda;

import com.condominiogestao.funcionario.Funcionario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Funcionário atribuído como responsável por uma demanda - N:N (uma demanda pode ter
 * mais de um responsável). Diferente do campo legado {@link Demanda#getFuncionarioResponsavel()}
 * (singular, nunca chegou a ganhar endpoint pra ser preenchido, continua exposto mas
 * morto) - esta tabela é o mecanismo de verdade pra atribuição a partir de agora.
 */
@Entity
@Table(name = "demanda_responsaveis", uniqueConstraints = @UniqueConstraint(columnNames = {"id_demanda", "id_funcionario"}))
@Getter
@Setter
@NoArgsConstructor
public class DemandaResponsavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atribuicao")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;
}
