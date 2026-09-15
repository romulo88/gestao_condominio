package com.condominiogestao.parametro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Parâmetro geral do sistema (pedido do Romulo) - valor de regra de negócio que o
 * administrador ajusta sem precisar de deploy (ver {@link ParametroService}). Global (não
 * é por condomínio) e único no sistema todo - {@code nome} é a "chave" que o código usa
 * pra ler o valor (ex: {@code ParametroService.getInt("maximoFotos", 3)}), por isso é
 * único e não é editável depois de criado (ver {@code ParametroUpdateRequest}) - trocar o
 * nome quebraria silenciosamente quem lê por esse nome.
 */
@Entity
@Table(name = "parametros", uniqueConstraints = @UniqueConstraint(columnNames = "nome"))
@Getter
@Setter
@NoArgsConstructor
public class Parametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Integer id;

    @Column(length = 100, nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /** Sempre texto no banco - quem lê decide o tipo (ver {@code ParametroService.getInt}
     * e afins). Deixa o parâmetro livre pra virar número, booleano etc. sem mudar schema. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String valor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
