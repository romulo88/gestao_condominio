package com.condominiogestao.tarefaagendada;

import com.condominiogestao.common.Situacao;
import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.funcionario.Funcionario;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Tarefa agendada: lembrete que qualquer funcionário do condomínio cadastra (ex: "renovar
 * seguro do elevador"), com três datas puras (sem hora) - a data da tarefa em si, a data
 * do primeiro aviso e a data do segundo aviso.
 *
 * <p>O "sininho" no menu do app fica vermelho quando <b>qualquer</b> uma das três datas de
 * <b>alguma</b> tarefa do condomínio cai em hoje, e a listagem vem ordenada por
 * {@link #proximaDataRelevante()} (a menor das três) - o que precisa de atenção primeiro
 * aparece no topo.
 */
@Entity
@Table(name = "tarefas_agendadas")
@Getter
@Setter
@NoArgsConstructor
public class TarefaAgendada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarefa_agendada")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    /** Quem cadastrou - qualquer funcionário do condomínio pode. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /** Data da tarefa em si (o primeiro dos três campos de data - só data, sem hora). */
    @Column(name = "data_tarefa", nullable = false)
    private LocalDate dataTarefa;

    /** Data do primeiro aviso. */
    @Column(name = "data_primeiro_aviso", nullable = false)
    private LocalDate dataPrimeiroAviso;

    /** Data do segundo aviso. */
    @Column(name = "data_segundo_aviso", nullable = false)
    private LocalDate dataSegundoAviso;

    /** "Remover" na aplicação (pedido do Romulo) - soft-delete, mesmo padrão de
     * Etiqueta/Aviso/MensagemRapida. A listagem só mostra as ativas. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Situacao situacao = Situacao.ativo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** A menor das três datas - é por ela que a listagem é ordenada (o que vence primeiro
     * fica no topo) e é o gancho do "sininho" (fica vermelho quando esta, ou qualquer uma
     * das três, é hoje - a checagem de "hoje" mesmo fica no cliente, que sabe o fuso do
     * usuário). */
    public LocalDate proximaDataRelevante() {
        return Stream.of(dataTarefa, dataPrimeiroAviso, dataSegundoAviso)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.MAX);
    }
}
