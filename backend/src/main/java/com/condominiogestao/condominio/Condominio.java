package com.condominiogestao.condominio;

import com.condominiogestao.common.Cnpj;
import com.condominiogestao.common.Situacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "condominios")
@Getter
@Setter
@NoArgsConstructor
public class Condominio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condominio")
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CondominioTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Situacao situacao = Situacao.ativo;

    /**
     * Só usado quando tipo = casas. Campo informativo/de referência - não gera
     * registros automaticamente em lugar nenhum.
     */
    @Column(name = "quantidade_casas")
    private Integer quantidadeCasas;

    /** Chave do arquivo no bucket S3-compatível (não uma URL pública - ver
     * {@code CondominioGifService}) - null quando o condomínio não tem GIF cadastrado.
     * Opcional, pedido do Romulo: exibido no lugar do título da página no Kanban. */
    @Column(name = "gif_url", columnDefinition = "TEXT")
    private String gifUrl;

    /** Token opaco do link público de leitura do Kanban (pedido do Romulo: "deixar o
     * kanban disponível em um link externo, independente do usuário estar logado") - null
     * quando o condomínio nunca gerou (ou já revogou) o link. Ver
     * {@code KanbanPublicoService}/{@code GET /api/kanban-publico/{token}}, que não exige
     * login nenhum - qualquer um com o link acessa, por isso o valor precisa ser
     * inadivinhável (gerado com {@code UUID.randomUUID()}, nunca sequencial). */
    @Column(name = "kanban_publico_token", unique = true)
    private String kanbanPublicoToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Rede de segurança: garante que o CNPJ fica salvo sem pontuação mesmo que algum
     * chamador esqueça de normalizar antes (ver {@link Cnpj#normalizar}). */
    @PrePersist
    @PreUpdate
    private void normalizarCnpj() {
        cnpj = Cnpj.normalizar(cnpj);
    }
}
