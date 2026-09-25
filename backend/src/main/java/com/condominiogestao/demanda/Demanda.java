package com.condominiogestao.demanda;

import com.condominiogestao.condominio.Condominio;
import com.condominiogestao.funcionario.Funcionario;
import com.condominiogestao.kanban.StatusKanban;
import com.condominiogestao.morador.Morador;
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
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entidade central do sistema. Origem: exatamente um entre {@code moradorSolicitante} /
 * {@code funcionarioSolicitante} deve estar preenchido - reforçado por
 * {@code chk_demanda_solicitante_unico} no banco (ver V1__init.sql), mas a aplicação
 * também deve validar antes de persistir, para dar um erro claro em vez de deixar
 * estourar a constraint.
 *
 * <p>Regra que o banco NÃO garante sozinho (validar no service): o funcionário/morador
 * solicitante, aprovador e responsável precisam ter vínculo ativo com o
 * {@code id_condominio} desta demanda; o {@link StatusKanban} referenciado também
 * precisa pertencer ao mesmo condomínio.
 */
@Entity
@Table(name = "demandas")
@Getter
@Setter
@NoArgsConstructor
public class Demanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demanda")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_condominio", nullable = false)
    private Condominio condominio;

    @Column(columnDefinition = "TEXT")
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_morador_solicitante")
    private Morador moradorSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_solicitante")
    private Funcionario funcionarioSolicitante;

    /** Só relevante quando solicitado por morador (item 4.2). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_aprovacao", length = 30)
    private DemandaStatusAprovacao statusAprovacao = DemandaStatusAprovacao.pendente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_aprovador")
    private Funcionario funcionarioAprovador;

    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    /** Enviada por e-mail ao morador quando reprovada. */
    @Column(name = "justificativa_reprovacao", columnDefinition = "TEXT")
    private String justificativaReprovacao;

    /** Pedido do Romulo: aprovar sem mandar pro Kanban - mesmo espírito de
     * {@code justificativaReprovacao}, só preenchida quando {@code statusKanban} fica
     * null na aprovação (ver {@code DemandaService.aprovar}). */
    @Column(name = "justificativa_aprovacao", columnDefinition = "TEXT")
    private String justificativaAprovacao;

    /**
     * Kanban - só existe depois de aprovada / entrar na fila. Aponta para uma coluna
     * cadastrada em {@link StatusKanban} do mesmo condomínio (validar isso na aplicação).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_status_kanban")
    private StatusKanban statusKanban;

    /** Posição do card dentro da coluna do Kanban (`statusKanban`) - pedido do Romulo:
     * poder arrastar um card pra qualquer posição, pra agrupar assuntos parecidos lado a
     * lado. Sempre 0..N-1 dentro da mesma coluna, renumerada inteira a cada
     * inserção/reordenação (ver {@code DemandaService.moverKanban}) - mesmo padrão de
     * {@link com.condominiogestao.demanda.DemandaEtapa#getOrdem()}, só que relativo à
     * coluna em vez da demanda. */
    private Integer ordem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_responsavel")
    private Funcionario funcionarioResponsavel;

    private boolean sigilosa = false;

    /** Pedido do Romulo: demanda numa coluna finalística ({@link StatusKanban#isFinalistico()})
     * pode ser arquivada pelo funcionário direto no card - sai do fluxo ativo sem apagar
     * nada (mesma filosofia de soft-delete do resto do sistema). Default false. */
    @Column(name = "arquivada", nullable = false)
    private boolean arquivada = false;

    /** Funcionário que marcou {@code sigilosa = true} mais recentemente (na criação ou
     * via {@code alternarSigilo}) - null se nunca foi marcada por um funcionário (ex:
     * morador marcou na própria criação) ou se está desmarcada agora. Item 4.8: essa
     * pessoa - além do solicitante e de síndico/sub-síndico - enxerga a demanda sigilosa
     * e pode indicar outras (ver {@code DemandaAcessoSigilosoService}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario_marcou_sigilo")
    private Funcionario funcionarioMarcouSigilo;

    /** Se o nome de quem abriu aparece pro funcionário no "Aberta por" da listagem - ver
     * {@code DemandaResponse.solicitanteNome}, que vem {@code null} quando isso é
     * {@code false}. Desmarcado por padrão pra morador, marcado por padrão pra
     * funcionário (decidido em {@code DemandaService.criar}, não aqui). */
    @Column(name = "identificar_solicitante")
    private boolean identificarSolicitante = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
