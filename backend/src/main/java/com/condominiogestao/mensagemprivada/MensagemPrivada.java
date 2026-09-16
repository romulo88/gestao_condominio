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
 * Uma linha (mensagem individual) dentro de uma {@link ConversaPrivada} - "conversa livre"
 * (pedido do Romulo): qualquer participante (autor original da conversa ou qualquer
 * destinatário) pode escrever novas mensagens, não só responder uma vez. Autor da mensagem
 * é sempre exatamente um entre {@link #moradorAutor}/{@link #funcionarioAutor}, reforçado por
 * {@code chk_mensagem_privada_autor_unico} (V22).
 */
@Entity
@Table(name = "mensagens_privadas")
@Getter
@Setter
@NoArgsConstructor
public class MensagemPrivada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensagem")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversa", nullable = false)
    private ConversaPrivada conversa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador_autor")
    private Morador moradorAutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_autor")
    private Funcionario funcionarioAutor;

    @Column(name = "texto", nullable = false)
    private String texto;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
