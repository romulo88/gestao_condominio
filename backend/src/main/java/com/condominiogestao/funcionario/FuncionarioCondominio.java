package com.condominiogestao.funcionario;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Vínculo funcionário {@literal <->} condomínio (N:N). Perfil e situação são POR
 * condomínio: o mesmo funcionário pode ser Síndico no condomínio A e Supervisor no B,
 * e pode ficar inativo em um sem afetar os demais.
 */
@Entity
@Table(name = "funcionarios_condominios",
       uniqueConstraints = @UniqueConstraint(columnNames = {"id_funcionario", "id_condominio"}))
@Getter
@Setter
@NoArgsConstructor
public class FuncionarioCondominio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vinculo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** null = sem acesso ao sistema NESSE condomínio (item 2.1). */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FuncionarioPerfil perfil;

    /** Texto livre pra descrever o que a pessoa faz (jardineiro, rondista, etc.) quando
     * ela não tem {@link #perfil} - pedido do Romulo, opcional (V23). */
    @Column(columnDefinition = "TEXT")
    private String funcao;

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
