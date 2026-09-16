package com.condominiogestao.mensagemprivada;

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
 * Foto anexada a uma {@link MensagemPrivada} - mesmo formato de {@code DemandaDocumento}
 * (nome original guardado só para exibição, {@link #url} é a chave no MinIO, nunca o nome
 * original - servido via endpoint proxy/assinado, nunca URL pública direta). Quantidade
 * máxima por mensagem é parametrizável (ver {@code ParametroService}, chave
 * {@code mensagemPrivadaMaximoFotos}), não fixa no código.
 */
@Entity
@Table(name = "mensagens_privadas_documentos")
@Getter
@Setter
@NoArgsConstructor
public class MensagemPrivadaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mensagem", nullable = false)
    private MensagemPrivada mensagem;

    @Column(name = "nome_arquivo")
    private String nomeArquivo;

    @Column(name = "url")
    private String url;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(name = "tamanho_bytes")
    private Integer tamanhoBytes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
