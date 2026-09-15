-- Pedido do Romulo: guardar quando cada pessoa logou pela última vez, pra dar suporte ao
-- alerta de mudança de status pro morador ("desde o último login") - e util de forma mais
-- geral pra qualquer feature futura que precise saber "há quanto tempo essa pessoa não
-- aparece por aqui". Nullable de propósito: pessoa que nunca logou (ou logou antes dessa
-- coluna existir) fica com null, sem retroatividade forçada.
ALTER TABLE pessoas ADD COLUMN ultimo_login TIMESTAMP;
