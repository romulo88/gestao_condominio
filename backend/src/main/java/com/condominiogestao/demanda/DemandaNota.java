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
 * Nota numa demanda (pedido do Romulo): depois de cadastrada, o morador pergunta sobre o
 * andamento e o funcionário responde com outra nota, se julgar necessário. {@link #notaPai}
 * aponta pra outra nota da MESMA demanda quando esta é uma resposta - é o que permite a
 * listagem vir indentada em relação à nota original (ver {@code DemandaNotaResponse}, a
 * indentação em si é responsabilidade do frontend a partir de {@code notaPaiId}).
 *
 * <p>Autor é sempre exatamente um entre {@link #morador}/{@link #funcionario} - reforçado
 * por {@code chk_demanda_nota_autor_unico} no banco (ver V14), mesmo padrão do solicitante
 * da demanda ({@link Demanda}).
 *
 * <p>{@link #lida}/{@link #lidaEm} - manual (ícone na listagem, só funcionário, ver
 * {@code DemandaNotaService#marcarLida}) ou automático em dois casos, os dois em
 * {@code DemandaNotaService#criar}: (1) a nota-pai vira lida quando um FUNCIONÁRIO
 * responde a ela ("responder já prova que leu"); (2) a resposta em si (qualquer nota que
 * já nasce com {@code notaPai} preenchido, de qualquer autor) já nasce lida - ela é a
 * própria resolução da pergunta que responde, não precisa que ninguém a "leia" depois.
 * Regra "só dá pra cadastrar nota enquanto a demanda não estiver arquivada" é checada no
 * service, não aqui.
 */
@Entity
@Table(name = "demanda_notas")
@Getter
@Setter
@NoArgsConstructor
public class DemandaNota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demanda", nullable = false)
    private Demanda demanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nota_pai")
    private DemandaNota notaPai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador")
    private Morador morador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario")
    private Funcionario funcionario;

    /** Máx. 150 caracteres - enforced no próprio banco (VARCHAR(150)), mesmo padrão de
     * {@code MensagemRapida.texto}. */
    @Column(length = 150, nullable = false)
    private String texto;

    private boolean lida = false;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
