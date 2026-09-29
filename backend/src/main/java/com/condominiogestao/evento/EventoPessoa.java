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
 * documento físico contra o que foi digitado. Foto (pedido do Romulo: registro de
 * segurança, opcional) é uma ação independente da liberação - ver {@code
 * EventoService.enviarFotoPessoa}. */
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

    /** Foto opcional (pedido do Romulo: registro de segurança), independente de
     * {@code liberado} - o porteiro pode tirar antes, depois ou nunca. Guarda a CHAVE do
     * objeto no bucket (mesmo padrão de {@code DemandaDocumento.url}), não uma URL. */
    @Column(name = "foto_chave")
    private String fotoChave;

    @Column(name = "foto_tipo_mime")
    private String fotoTipoMime;

    @Column(name = "foto_tamanho_bytes")
    private Integer fotoTamanhoBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_foto")
    private Funcionario funcionarioFoto;

    @Column(name = "foto_em")
    private LocalDateTime fotoEm;
}
