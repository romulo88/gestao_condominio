package com.condominiogestao.aviso;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.funcionario.Funcionario;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Quadro de avisos: comunicado geral do condomínio, mantido pelos funcionários (ex:
 * "Piscina interditada devido a vazamentos"). Ideia é reduzir demandas desnecessárias -
 * a pessoa já vê o aviso ao logar, em vez de abrir uma demanda pra algo já sabido.
 *
 * <p>Um aviso fica "visível" quando {@code situacao = ativo} <b>e</b> ({@code dataExpiracao}
 * é nula <b>ou</b> ainda não passou) - ver {@link AvisoRepository#findVisiveisPorCondominio}.
 * {@code situacao} existe separado da expiração pra cobrir o caso de resolver o problema
 * antes do prazo (ex: piscina liberada no dia 2, mas a expiração era pro dia 5).
 */
@Entity
@Table(name = "avisos")
@Getter
@Setter
@NoArgsConstructor
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aviso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** Quem postou - só funcionário pode manter o quadro de avisos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    /** Máx. 250 caracteres - enforced no próprio banco (VARCHAR(250)). */
    @Column(length = 250)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Situacao situacao = Situacao.ativo;

    /** Opcional - aviso sem prazo (ex: regras permanentes) fica visível até ser desativado. */
    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;

    /** Reservado pra 1 aviso de "informações úteis" (ex: telefones da administração) que
     * deve sempre aparecer primeiro no quadro - pedido do Romulo. Só 1 por condomínio ao
     * mesmo tempo: garantido em {@link AvisoService#fixarNoTopo} (desfixa o anterior antes
     * de fixar o novo) e reforçado no banco por um índice único parcial (V24). */
    @Column(name = "fixado_no_topo", nullable = false)
    private boolean fixadoNoTopo = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
