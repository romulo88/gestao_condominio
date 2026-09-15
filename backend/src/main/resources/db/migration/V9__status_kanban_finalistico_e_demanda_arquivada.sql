-- Coluna do Kanban pode ser marcada como "finalística" (pedido do Romulo) - representa
-- uma situação terminal do fluxo (ex: "Finalizada", "Cancelada"). Só demanda numa coluna
-- assim pode ser arquivada. Default false: nenhuma coluna existente vira finalística sem o
-- síndico marcar explicitamente (Postgres backfilla as linhas atuais com o default ao
-- adicionar a coluna NOT NULL).
ALTER TABLE status_kanban ADD COLUMN finalistico BOOLEAN NOT NULL DEFAULT false;

-- Demanda arquivada (pedido do Romulo) - marcada pelo funcionário no card quando a demanda
-- está numa coluna finalística. Não apaga nada, só tira do fluxo ativo (mesma filosofia de
-- soft-delete do resto do sistema). Default false: toda demanda existente continua não
-- arquivada.
ALTER TABLE demandas ADD COLUMN arquivada BOOLEAN NOT NULL DEFAULT false;
