package com.condominiogestao.evento;

import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.espacocomum.EspacoComum;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Evento cadastrado pelo morador (festa/visita, pedido do Romulo) - substitui o aviso por
 * WhatsApp pra portaria. {@code espacoComum} nulo significa "própria unidade do morador";
 * preenchido, um dos {@link EspacoComum} do condomínio. Não é sistema de reserva - sem
 * checagem de conflito entre eventos no mesmo espaço/dia, é só um registro pra portaria
 * consultar. Sem coleção de {@link EventoVeiculo}/{@link EventoPessoa} aqui - mesmo padrão
 * de {@code Ronda}/{@code RondaPonto}, cada filho só aponta de volta e é buscado pelo
 * próprio repository.
 */
@Entity
@Table(name = "eventos")
@Getter
@Setter
@NoArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador", nullable = false)
    private Morador morador;

    /** Nulo = própria unidade do morador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_espaco_comum")
    private EspacoComum espacoComum;

    @Column(length = 200, nullable = false)
    private String motivo;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    /** Texto livre (ex.: "14h às 20h") - só informativo pra portaria, sem validação de intervalo. */
    @Column(length = 50)
    private String horario;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
