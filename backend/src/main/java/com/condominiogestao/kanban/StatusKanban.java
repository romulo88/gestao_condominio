package com.condominiogestao.kanban;

import com.condominiogestao.condominio.Condominio;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Coluna do quadro Kanban - cadastrada por condomínio (cada condomínio define as suas,
 * não é mais uma lista fixa no sistema todo). {@code ordem} define a posição de
 * exibição no quadro.
 */
@Entity
@Table(name = "status_kanban", uniqueConstraints = @UniqueConstraint(columnNames = {"id_condominio", "nome"}))
@Getter
@Setter
@NoArgsConstructor
public class StatusKanban {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_status_kanban")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** ex: "Fila", "Em andamento" - livre, definido pelo condomínio */
    @Column(columnDefinition = "TEXT")
    private String nome;

    private Integer ordem = 0;

    /** Pedido do Romulo: coluna (e as demandas nela) pode ficar oculta pro morador -
     * default true (visível), preservando o que já existe. */
    @Column(name = "visivel_externamente", nullable = false)
    private boolean visivelExternamente = true;

    /** Pedido do Romulo: coluna que representa uma situação terminal do fluxo (ex:
     * "Finalizada", "Cancelada"). Só demanda numa coluna finalística pode ser arquivada
     * ({@link com.condominiogestao.demanda.Demanda#isArquivada()}). Default false - nenhuma
     * coluna é finalística até o síndico marcar. */
    @Column(name = "finalistico", nullable = false)
    private boolean finalistico = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
