package com.condominiogestao.mensagemprivada;

import com.condominiogestao.funcionario.Funcionario;
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
 * Log de auditoria (pedido do Romulo) - uma linha por abertura de {@link ConversaPrivada},
 * append-only (nunca atualizada nem apagada), pra permitir investigar quem viu uma conversa
 * e quando em caso de suspeita de vazamento. Diferente de {@link
 * ConversaPrivada#getAutorUltimaVisualizacaoEm()}/{@link
 * ConversaPrivadaDestinatario#getUltimaVisualizacaoEm()} - que guardam só a última
 * visualização, sobrescrita a cada abertura e usada pro cálculo de "pendente" - esta
 * entidade é histórico completo, sem relação com aquele cálculo.
 */
@Entity
@Table(name = "conversas_privadas_visualizacoes")
@Getter
@Setter
@NoArgsConstructor
public class ConversaPrivadaVisualizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_visualizacao")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversa", nullable = false)
    private ConversaPrivada conversa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador")
    private Morador morador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario")
    private Funcionario funcionario;

    @CreationTimestamp
    @Column(name = "visualizado_em", updatable = false)
    private LocalDateTime visualizadoEm;
}
