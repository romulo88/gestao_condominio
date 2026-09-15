-- Atribuição de responsáveis pela demanda (N:N - pode ter mais de um funcionário) -
-- pedido do Romulo pro cadastro de demandas: formulário de atribuir/remover parecido
-- com o de acesso sigiloso (item 4.8), buscando por CPF ou nome. Diferente do campo
-- legado demandas.id_funcionario_responsavel (singular, nunca chegou a ganhar endpoint
-- pra ser preenchido) - esta tabela é o mecanismo de verdade daqui pra frente.
CREATE TABLE demanda_responsaveis (
    id_atribuicao  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demanda     INTEGER NOT NULL REFERENCES demandas (id_demanda),
    id_funcionario INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    UNIQUE (id_demanda, id_funcionario)
);
