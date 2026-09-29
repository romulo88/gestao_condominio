-- Foto opcional da pessoa ao ser liberada (pedido do Romulo: registro de segurança - em
-- caso de mal-feito, dá pra identificar quem entrou). Ação independente da liberação -
-- por isso fica em colunas nullable direto em evento_pessoas (1 foto por pessoa, sem
-- galeria), não numa tabela filha como demanda_documentos.
ALTER TABLE evento_pessoas ADD COLUMN foto_chave TEXT;
ALTER TABLE evento_pessoas ADD COLUMN foto_tipo_mime TEXT;
ALTER TABLE evento_pessoas ADD COLUMN foto_tamanho_bytes INTEGER;
ALTER TABLE evento_pessoas ADD COLUMN id_funcionario_foto INTEGER REFERENCES funcionarios (id_funcionario);
ALTER TABLE evento_pessoas ADD COLUMN foto_em TIMESTAMP;
