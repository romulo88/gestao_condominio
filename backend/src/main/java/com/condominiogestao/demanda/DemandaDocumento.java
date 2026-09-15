package com.condominiogestao.demanda;

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
 * Anexos (fotos, PDFs) - item 4.9. Arquivo em si fica em storage externo (MinIO local /
 * Cloudflare R2 em produção - ver docs/modelo-dados.md); aqui só a referência.
 */
@Entity
@Table(name = "demanda_documentos")
@Getter
@Setter
@NoArgsConstructor
public class DemandaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @Column(name = "nome_arquivo", columnDefinition = "TEXT")
    private String nomeArquivo;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "tipo_mime", columnDefinition = "TEXT")
    private String tipoMime;

    @Column(name = "tamanho_bytes")
    private Integer tamanhoBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador_upload")
    private Morador moradorUpload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_upload")
    private Funcionario funcionarioUpload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
