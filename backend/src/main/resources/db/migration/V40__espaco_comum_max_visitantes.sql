-- Quantidade máxima de visitantes de um espaço de lazer (pedido do Romulo): aparece pro
-- morador, num aviso fixo no topo, quando ele escolhe esse espaço ao cadastrar um evento.
-- Nullable de propósito: os espaços que já existem não têm esse dado (o aviso só aparece
-- quando preenchido) - ninguém é obrigado a inventar um número pra espaço antigo.
ALTER TABLE espacos_comuns ADD COLUMN max_visitantes INTEGER CHECK (max_visitantes IS NULL OR max_visitantes > 0);
