-- Prova de que o indice do Kanban funciona sob volume, SEM sujar o banco:
-- tudo roda dentro de uma transacao que termina em ROLLBACK.
--
--   cmd /c "docker compose exec -T postgres psql -U condominio -d condominio_gestao -f - < docs\verificar-indices-carga.sql"

BEGIN;

-- 50 mil demandas sinteticas no condominio 1, com created_at espalhado no tempo.
INSERT INTO demandas (id_condominio, titulo, descricao, id_morador_solicitante,
                      status_aprovacao, created_at, updated_at)
SELECT 1,
       'carga sintetica ' || g,
       'linha de teste, sera descartada no rollback',
       (SELECT id_morador FROM moradores ORDER BY id_morador LIMIT 1),
       'pendente',
       now() - (g || ' minutes')::interval,
       now()
FROM generate_series(1, 50000) g;

ANALYZE demandas;

\echo ''
\echo '=== COM o indice (consulta real do Kanban, 50 primeiras) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM demandas WHERE id_condominio = 1 ORDER BY created_at DESC LIMIT 50;

\echo ''
\echo '=== SEM o indice (mesmo dado, planner proibido de usar indice) ==='
SET enable_indexscan = off;
SET enable_bitmapscan = off;
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM demandas WHERE id_condominio = 1 ORDER BY created_at DESC LIMIT 50;
RESET enable_indexscan;
RESET enable_bitmapscan;

-- Nada acima e persistido.
ROLLBACK;

\echo ''
\echo '=== Confirmacao: contagem de volta ao normal apos o rollback ==='
SELECT count(*) AS demandas_reais FROM demandas;
