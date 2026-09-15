package com.condominiogestao.condominio;

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
 * Só existe para condomínios do tipo "apartamento" (item 1.1) - tem tela própria de cadastro.
 */
@Entity
@Table(name = "blocos", uniqueConstraints = @UniqueConstraint(columnNames = {"id_condominio", "nome"}))
@Getter
@Setter
@NoArgsConstructor
public class Bloco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloco")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** ex: "Bloco A", "Torre 1" */
    @Column(columnDefinition = "TEXT")
    private String nome;
}
