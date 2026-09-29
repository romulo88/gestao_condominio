package com.condominiogestao.evento;

import com.condominiogestao.funcionario.Funcionario;
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

/** Uma pessoa esperada num {@link Evento} - liberação por item (ver {@link EventoService}).
 * {@code documento} é texto livre (ex.: "RG 12.345.678-9") - a portaria confere o
 * documento físico contra o que foi digitado, sem foto/upload nesta feature. */
@Entity
@Table(name = "evento_pessoas")
@Getter
@Setter
@NoArgsConstructor
public class EventoPessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_pessoa")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(length = 120, nullable = false)
    private String nome;

    @Column(length = 50, nullable = false)
    private String documento;

    @Column(nullable = false)
    private boolean liberado = false;

    /** Só preenchido enquanto {@code liberado = true} - ver toggle em {@code EventoService.liberarPessoa}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_liberou")
    private Funcionario funcionarioLiberou;

    @Column(name = "liberado_em")
    private LocalDateTime liberadoEm;
}
