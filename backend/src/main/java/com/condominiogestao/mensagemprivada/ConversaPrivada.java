package com.condominiogestao.mensagemprivada;

import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.morador.Morador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Conversa privada (pedido do Romulo) - criada por morador OU funcionário, endereçada a um
 * ou mais funcionários com login do mesmo condomínio (ver {@link ConversaPrivadaDestinatario}).
 * Diferente de {@code DemandaNota}: não está atrelada a nenhuma demanda, e só quem participa
 * (autor + destinatários) enxerga - nem síndico por regra, nem qualquer outro funcionário do
 * condomínio. É o "container" da conversa; cada linha escrita é uma {@link MensagemPrivada}.
 *
 * <p>Autor é sempre exatamente um entre {@link #moradorAutor}/{@link #funcionarioAutor} -
 * reforçado por {@code chk_conversa_privada_autor_unico} no banco (ver V22), mesmo padrão do
 * solicitante de {@code Demanda}.
 *
 * <p>{@link #autorUltimaVisualizacaoEm} + a mesma coluna em {@link ConversaPrivadaDestinatario}
 * (uma por destinatário) substituem um flag "lida" por mensagem - "tem coisa nova pra eu ver"
 * é calculado comparando essa data com a data da mensagem mais recente da conversa (ver
 * {@code ConversaPrivadaService#pendente}), não guardado por linha.
 */
@Entity
@Table(name = "conversas_privadas")
@Getter
@Setter
@NoArgsConstructor
public class ConversaPrivada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversa")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador_autor")
    private Morador moradorAutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_autor")
    private Funcionario funcionarioAutor;

    @Column(name = "autor_ultima_visualizacao_em")
    private LocalDateTime autorUltimaVisualizacaoEm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
