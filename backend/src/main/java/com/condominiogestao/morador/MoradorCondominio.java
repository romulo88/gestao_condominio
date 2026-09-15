package com.condominiogestao.morador;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Bloco;
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
 * Vínculo morador {@literal <->} condomínio (N:N). Quem cadastra o morador é sempre um
 * funcionário, que já sabe o bloco/unidade de cabeça - por isso ficam como campos
 * simples aqui em vez de uma tabela Unidade à parte. Situação é POR vínculo: o síndico
 * de um condomínio só inativa o morador ali, sem afetar vínculos em outros condomínios.
 * Também cobre o caso de mais de um morador na mesma unidade (ex: família) - cada um
 * com sua própria linha.
 */
@Entity
@Table(name = "moradores_condominios")
@Getter
@Setter
@NoArgsConstructor
public class MoradorCondominio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vinculo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador", nullable = false)
    private Morador morador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** Preenchido só quando o condomínio é de apartamentos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bloco")
    private Bloco bloco;

    /** Número do apto ou da casa, texto livre digitado pelo funcionário. */
    @Column(name = "numero_unidade", columnDefinition = "TEXT")
    private String numeroUnidade;

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
