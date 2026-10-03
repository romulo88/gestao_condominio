-- Reforma na própria unidade (pedido do Romulo): morador marca o evento como reforma e
-- informa início/fim (até 15 dias de intervalo) - o service cria um evento por dia do
-- intervalo, cada um com a mesma pessoa/veículo esperados, pra portaria liberar dia a dia
-- (não existe conceito de "evento de vários dias" no modelo, ver V34). `reforma` só fica
-- true quando o evento é na própria unidade (id_espaco_comum nulo) - validado no
-- EventoService, não no banco.
ALTER TABLE eventos ADD COLUMN reforma BOOLEAN NOT NULL DEFAULT false;
