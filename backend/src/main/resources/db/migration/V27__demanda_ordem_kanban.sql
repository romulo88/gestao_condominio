-- Reordenação manual de cards dentro de uma coluna do Kanban (pedido do Romulo) - mesmo
-- padrão de `demanda_etapas.ordem`, só que relativo à coluna (`status_kanban`) em vez da
-- demanda. Backfill por coluna, preservando a ordem visual atual (created_at desc), pra
-- ninguém ver os cards embaralhados na primeira tela depois do restart.
ALTER TABLE demandas ADD COLUMN ordem INTEGER NOT NULL DEFAULT 0;

UPDATE demandas d
SET ordem = sub.rn - 1
FROM (
    SELECT id_demanda, ROW_NUMBER() OVER (
        PARTITION BY id_status_kanban ORDER BY created_at DESC
    ) AS rn
    FROM demandas
    WHERE id_status_kanban IS NOT NULL
) sub
WHERE d.id_demanda = sub.id_demanda;
