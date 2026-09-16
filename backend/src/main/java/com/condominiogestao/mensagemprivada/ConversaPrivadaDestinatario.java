package com.condominiogestao.mensagemprivada;

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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Destinatário (N:N) de uma {@link ConversaPrivada} - permite endereçar a mais de um
 * funcionário ao mesmo tempo (ex: síndico e sub-síndico, pedido do Romulo). {@link
 * #ultimaVisualizacaoEm} é individual por destinatário: cada um controla o próprio estado de
 * "visto", independente dos demais e do autor ({@code ConversaPrivada#autorUltimaVisualizacaoEm}).
 */
@Entity
@Table(name = "conversas_privadas_destinatarios")
@Getter
@Setter
@NoArgsConstructor
public class ConversaPrivadaDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_destinatario")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversa", nullable = false)
    private ConversaPrivada conversa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(name = "ultima_visualizacao_em")
    private LocalDateTime ultimaVisualizacaoEm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
