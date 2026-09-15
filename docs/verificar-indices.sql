-- Verificacao da migration V17 (indices de performance).
-- Rodar com (a partir da RAIZ do projeto, nao de backend/):
--   cmd /c "docker compose exec -T postgres psql -U condominio -d condominio_gestao -f - < docs\verificar-indices.sql"

\echo '=== 1. Os 9 indices do V17 existem? ==='
SELECT esperado.nome,
       CASE WHEN i.indexname IS NULL THEN '*** FALTANDO ***' ELSE 'ok' END AS situacao,
       pg_size_pretty(COALESCE(pg_relation_size(('public.' || i.indexname)::regclass), 0)) AS tamanho
FROM (VALUES
        ('idx_demandas_condominio_created_at'),
        ('idx_demandas_morador_data_aprovacao'),
        ('idx_demanda_etapas_demanda_ordem'),
        ('idx_demanda_documentos_demanda'),
        ('idx_demanda_hist_kanban_demanda_created_at'),
        ('idx_moradores_condominios_morador_condominio'),
        ('idx_moradores_condominios_condominio'),
        ('idx_funcionarios_condominios_condominio'),
        ('idx_avisos_condominio_created_at')
     ) AS esperado(nome)
LEFT JOIN pg_indexes i ON i.indexname = esperado.nome AND i.schemaname = 'public'
ORDER BY situacao DESC, esperado.nome;

\echo ''
\echo '=== 2. Flyway registrou a V17 com sucesso? ==='
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;

\echo ''
\echo '=== 3. Indice invalido? (CREATE INDEX que falhou no meio deixa o indice invalido) ==='
SELECT c.relname AS indice_invalido
FROM pg_index x
JOIN pg_class c ON c.oid = x.indexrelid
WHERE NOT x.indisvalid;

\echo ''
\echo '=== 4. Volume atual das tabelas quentes ==='
SELECT 'demandas' AS tabela, count(*) FROM demandas
UNION ALL SELECT 'demanda_etapas', count(*) FROM demanda_etapas
UNION ALL SELECT 'demanda_documentos', count(*) FROM demanda_documentos
UNION ALL SELECT 'moradores_condominios', count(*) FROM moradores_condominios
UNION ALL SELECT 'avisos', count(*) FROM avisos;

\echo ''
\echo '=== 5. O planner usa o indice na consulta do Kanban? ==='
\echo '(com poucas linhas, Seq Scan aqui e o comportamento CORRETO - ver nota no final)'
ANALYZE;
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM demandas WHERE id_condominio = 1 ORDER BY created_at DESC;
