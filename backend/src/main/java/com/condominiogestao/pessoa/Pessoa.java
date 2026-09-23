package com.condominiogestao.pessoa;

import com.condominiogestao.common.Cpf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Identidade da pessoa (nome, cpf, email, senha) - compartilhada entre os papéis que
 * ela pode ter no sistema. {@code Funcionario} e {@code Morador} são extensões 1:1
 * dessa tabela (mesma PK, não herança Java): a mesma pessoa pode ser as duas coisas ao
 * mesmo tempo (ex: síndico que também mora no condomínio), cada papel com sua própria
 * linha em {@code funcionarios}/{@code moradores} apontando pra cá.
 */
@Entity
@Table(name = "pessoas")
@Getter
@Setter
@NoArgsConstructor
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pessoa")
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String nome;

    /** Login (para quem tiver papel de funcionário com perfil, ou de morador). */
    @Column(columnDefinition = "TEXT")
    private String cpf;

    @Column(columnDefinition = "TEXT")
    private String email;

    /** bcrypt/argon2 - nunca texto puro. */
    @Column(name = "senha_hash", columnDefinition = "TEXT")
    private String senhaHash;

    /** Nasce {@code true} - toda pessoa é criada com uma senha provisória (código
     * temporário gerado por {@code SenhaProvisoriaService} e mandado por e-mail, se
     * cadastrada com um), que nunca serve pra logar de verdade. Só o fluxo guiado
     * (`POST /api/auth/esqueci-senha` + `/api/auth/trocar-senha`) desliga essa flag - ver
     * {@code AuthService.login}. */
    @Column(name = "precisa_trocar_senha")
    private boolean precisaTrocarSenha = true;

    /** Chave do arquivo no bucket S3-compatível (não uma URL pública - ver
     * {@code PessoaFotoService}) - null quando a pessoa não tem foto cadastrada. Só uma
     * foto por pessoa, por isso fica aqui e não duplicada em Funcionario/Morador. */
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    /** Preenchido pelo `AuthService.login` a cada login de verdade (não a troca de perfil,
     * que é a mesma sessão) - pedido do Romulo pra dar suporte ao alerta de mudança de
     * status pro morador ("desde o último login"). Null até a primeira vez que a pessoa
     * loga depois dessa coluna existir. */
    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Rede de segurança: garante que o CPF fica salvo sem pontuação mesmo que algum
     * chamador esqueça de normalizar antes (ver {@link Cpf#normalizar}). */
    @PrePersist
    @PreUpdate
    private void normalizarCpf() {
        cpf = Cpf.normalizar(cpf);
    }
}
