package com.condominiogestao.etiqueta;

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
 * Etiqueta para classificar demandas - cadastrada por condomínio, igual StatusKanban.
 * Inativar uma etiqueta não desfaz vínculos já existentes em {@link DemandaEtiqueta}
 * - só deve deixar de aparecer como opção para adicionar em demandas novas
 * (regra de aplicação, não do banco).
 */
@Entity
@Table(name = "etiquetas", uniqueConstraints = @UniqueConstraint(columnNames = {"id_condominio", "descricao"}))
@Getter
@Setter
@NoArgsConstructor
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etiqueta")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** Máx. 50 caracteres - enforced no próprio banco (VARCHAR(50)). */
    @Column(length = 50)
    private String descricao;

    /** Cor em hex (ex: "#2F80ED") - validar formato na aplicação. */
    @Column(columnDefinition = "TEXT")
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Situacao situacao = Situacao.ativo;

    /** Etiqueta pode ficar oculta pro morador (pedido do Romulo) - mesmo padrão de
     * {@link com.condominiogestao.kanban.StatusKanban#isVisivelExternamente()}: funcionário
     * sempre vê/gerencia todas, só o morador é filtrado (ver {@code DemandaService.listar}). */
    @Column(name = "visivel_morador", nullable = false)
    private boolean visivelMorador = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
