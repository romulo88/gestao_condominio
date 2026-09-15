-- Aprovar uma demanda sem mandar pro Kanban (pedido do Romulo: "não tem como aprovar uma
-- demanda sem ela ir para o Kanban... quero que, assim como a reprovação, tenha uma
-- justificativa e não precise escolher um status_kanban"). Mesmo padrão de
-- justificativa_reprovacao - texto livre, opcional (só preenchida quando a aprovação
-- acontece sem coluna de Kanban escolhida).
ALTER TABLE demandas ADD COLUMN justificativa_aprovacao TEXT;
