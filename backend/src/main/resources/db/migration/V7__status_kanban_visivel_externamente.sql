-- Coluna do Kanban pode ficar oculta pro morador (pedido do Romulo) - útil pra colunas
-- internas/administrativas que não fazem sentido expor pra fora do condomínio (ex:
-- "Aguardando aprovação jurídica"). Quando false, a coluna E as demandas nela somem do
-- Kanban do morador (funcionário continua vendo tudo). Default true preserva o
-- comportamento atual pra quem já existe - toda coluna cadastrada até agora continua
-- visível sem precisar reconfigurar nada (Postgres já backfilla as linhas existentes com
-- o valor default ao adicionar a coluna NOT NULL).
ALTER TABLE status_kanban ADD COLUMN visivel_externamente BOOLEAN NOT NULL DEFAULT true;
