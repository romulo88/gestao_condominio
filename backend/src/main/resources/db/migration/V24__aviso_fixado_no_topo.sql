-- Aviso fixado no topo (pedido do Romulo): reservado pra 1 aviso de "informações úteis"
-- (ex: telefones da administração) que deve sempre aparecer primeiro no quadro, não
-- importa a data de criação. Default false - todo aviso já existente continua ordenado só
-- por data (comportamento de sempre).
ALTER TABLE avisos ADD COLUMN fixado_no_topo BOOLEAN NOT NULL DEFAULT false;

-- Só 1 fixado por condomínio ao mesmo tempo - reforçado aqui (índice único parcial,
-- padrão Postgres pra "único só quando uma condição bate") além de já ser garantido em
-- Java (AvisoService.fixarNoTopo desfixa o anterior antes de fixar o novo, na mesma
-- transação) - dupla proteção contra uma corrida de duas requisições simultâneas.
CREATE UNIQUE INDEX idx_avisos_fixado_unico ON avisos (id_condominio) WHERE fixado_no_topo = true;
