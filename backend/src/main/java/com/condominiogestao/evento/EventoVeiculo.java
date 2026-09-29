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

/** Um veículo esperado num {@link Evento} - liberação por item (ver {@link EventoService}). */
@Entity
@Table(name = "evento_veiculos")
@Getter
@Setter
@NoArgsConstructor
public class EventoVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_veiculo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(length = 8, nullable = false)
    private String placa;

    @Column(nullable = false)
    private boolean liberado = false;

    /** Só preenchido enquanto {@code liberado = true} - ver toggle em {@code EventoService.liberarVeiculo}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_liberou")
    private Funcionario funcionarioLiberou;

    @Column(name = "liberado_em")
    private LocalDateTime liberadoEm;
}
