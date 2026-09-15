package com.condominiogestao.mensagemrapida;

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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Mensagem pronta pra funcionário reaproveitar (ex: respostas padrão de demanda) -
 * cadastrada por condomínio, igual {@link com.condominiogestao.etiqueta.Etiqueta}. Excluir
 * na aplicação é soft-delete ({@code situacao = inativo}, ver {@link MensagemRapidaService})
 * - a listagem só mostra as ativas.
 */
@Entity
@Table(name = "mensagens_rapidas")
@Getter
@Setter
@NoArgsConstructor
public class MensagemRapida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensagem_rapida")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** Máx. 150 caracteres - enforced no próprio banco (VARCHAR(150)). */
    @Column(length = 150)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MensagemRapidaCarater carater;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Situacao situacao = Situacao.ativo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
