package com.condominiogestao.demanda;

import com.condominiogestao.common.TipoPessoa;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.morador.Morador;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Controle de quem pode ver uma demanda sigilosa (item 4.8), além de quem a criou.
 * Regra de negócio na aplicação: se {@code demanda.sigilosa = true}, só o solicitante,
 * o responsável atual, e quem estiver nessa tabela podem visualizar.
 */
@Entity
@Table(
    name = "demanda_acesso_sigiloso",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"id_demanda", "tipo_pessoa", "id_morador", "id_funcionario"}))
@Getter
@Setter
@NoArgsConstructor
public class DemandaAcessoSigiloso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acesso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 30)
    private TipoPessoa tipoPessoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador")
    private Morador morador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario")
    private Funcionario funcionario;
}
