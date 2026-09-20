-- Coluna do Kanban pode ser marcada como "recorrente" (pedido do Romulo) - agrupa demandas
-- diárias que ficam ali indefinidamente (ex: "Limpeza", "Portaria", "Ronda"). Serve pra
-- excluir essas colunas (e as finalísticas) do futuro dashboard de tempo parado, já que não
-- faz sentido contar tempo de algo que nunca "termina". Default false: nenhuma coluna
-- existente vira recorrente sem o síndico marcar explicitamente.
ALTER TABLE status_kanban ADD COLUMN recorrente BOOLEAN NOT NULL DEFAULT false;
