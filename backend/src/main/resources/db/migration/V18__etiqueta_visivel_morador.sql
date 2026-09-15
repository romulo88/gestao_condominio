-- Etiqueta pode ficar oculta pro morador (pedido do Romulo) - útil pra classificação
-- interna do funcionário (ex: "Prioridade jurídica") que não faz sentido expor pra fora
-- do condomínio. Mesmo padrão de `status_kanban.visivel_externamente` (V7): funcionário
-- continua vendo e gerenciando todas, só o morador é filtrado. Default true preserva o
-- comportamento pedido pelo Romulo pra quem já existe - toda etiqueta cadastrada até
-- agora continua visível sem precisar reconfigurar nada (Postgres já backfilla as linhas
-- existentes com o valor default ao adicionar a coluna NOT NULL).
ALTER TABLE etiquetas ADD COLUMN visivel_morador BOOLEAN NOT NULL DEFAULT true;
