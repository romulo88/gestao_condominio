-- Log de auditoria de visualização de conversa privada (pedido do Romulo: se uma mensagem
-- vazar, poder rastrear quem abriu aquela conversa e quando, pra restringir a investigação).
-- Diferente de conversas_privadas.autor_ultima_visualizacao_em/conversas_privadas_destinatarios.
-- ultima_visualizacao_em (V22) - que guardam só a ÚLTIMA vez, sobrescritas a cada abertura,
-- usadas pro cálculo de "pendente" - esta tabela é append-only: uma linha por abertura,
-- nunca atualizada nem apagada. Primeira tabela de log do projeto (mais próximo que existia
-- era demanda_status_kanban_historico, que registra mudança de status, não visualização).
CREATE TABLE conversas_privadas_visualizacoes (
    id_visualizacao INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conversa     INTEGER NOT NULL REFERENCES conversas_privadas (id_conversa),
    id_morador      INTEGER REFERENCES moradores (id_morador),
    id_funcionario  INTEGER REFERENCES funcionarios (id_funcionario),
    visualizado_em  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_conversa_privada_visualizacao_pessoa_unica CHECK (
        (id_morador IS NOT NULL) <> (id_funcionario IS NOT NULL)
    )
);

CREATE INDEX idx_conversas_privadas_visualizacoes_conversa ON conversas_privadas_visualizacoes (id_conversa);
