package com.condominiogestao.administrador;

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
 * Papel GLOBAL de uma {@link Pessoa} - extensão 1:1 (mesma PK, via {@code @MapsId}),
 * igual {@code Funcionario}/{@code Morador}. Diferença: não tem vínculo com nenhum
 * condomínio específico (não existe "administradores_condominios") - é quem opera o
 * sistema como um todo (cadastra condomínios, etc.), não quem administra um prédio
 * específico (isso é o síndico, um perfil de {@code Funcionario}).
 */
@Entity
@Table(name = "administradores")
@Getter
@Setter
@NoArgsConstructor
public class Administrador {

    @Id
    @Column(name = "id_administrador")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_administrador")
    private Pessoa pessoa;

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

    public String getEmail() {
        return pessoa.getEmail();
    }
}
