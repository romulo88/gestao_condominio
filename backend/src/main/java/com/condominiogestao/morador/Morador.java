package com.condominiogestao.morador;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.pessoa.Pessoa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Papel de morador de uma {@link Pessoa} - extensão 1:1 (mesma PK, via
 * {@code @MapsId}), não herança Java: a mesma pessoa pode ter também um papel de
 * {@code Funcionario} ao mesmo tempo (ex: síndico que também mora no condomínio). Nome,
 * cpf, email e senha ficam em {@link Pessoa} - usar {@link #getNome()}/{@link #getCpf()}
 * /{@link #getEmail()} como atalho.
 */
@Entity
@Table(name = "moradores")
@Getter
@Setter
@NoArgsConstructor
public class Morador {

    @Id
    @Column(name = "id_morador")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_morador")
    private Pessoa pessoa;

    /**
     * Desativação GLOBAL: inativa o morador em TODOS os condomínios de uma vez. Para
     * inativar em apenas um condomínio, usar {@link MoradorCondominio#getSituacao()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Situacao situacao = Situacao.ativo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getNome() {
        return pessoa.getNome();
    }

    public String getCpf() {
        return pessoa.getCpf();
    }

    public String getEmail() {
        return pessoa.getEmail();
    }
}
