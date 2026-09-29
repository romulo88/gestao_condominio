package com.condominiogestao.espacocomum;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Espaço de lazer do condomínio (quiosque, salão gourmet, salão de festas, ...) -
 * cadastrado por condomínio, mesmo padrão de {@link com.condominiogestao.etiqueta.Etiqueta}.
 * Usado como opção de "local" ao cadastrar um {@link com.condominiogestao.evento.Evento}
 * (pedido do Romulo: aba nova em Condomínio, porque cada condomínio tem um conjunto
 * diferente de espaços).
 */
@Entity
@Table(name = "espacos_comuns", uniqueConstraints = @UniqueConstraint(columnNames = {"id_condominio", "nome"}))
@Getter
@Setter
@NoArgsConstructor
public class EspacoComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_espaco_comum")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    @Column(length = 50, nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private Situacao situacao = Situacao.ativo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
