-- Cadastro de tarefas agendadas (pedido do Romulo): qualquer funcionário do condomínio
-- registra uma tarefa com título, descrição e TRÊS datas - a data da tarefa em si, a data
-- do primeiro aviso e a data do segundo aviso. São datas puras (DATE, sem hora) porque o
-- que importa é o dia: o "sininho" no menu fica vermelho quando qualquer uma das três cai
-- em HOJE, e a listagem vem ordenada pela data mais próxima entre as três.
--
-- Escopo desta leva: criar + listar. Sem editar/concluir/remover (não foi pedido) - se
-- precisar, entra depois, no mesmo espírito soft-delete do resto do sistema.
CREATE TABLE tarefas_agendadas (
    id_tarefa_agendada  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio       INTEGER NOT NULL REFERENCES condominios (id_condominio),
    id_funcionario      INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    titulo              TEXT NOT NULL,
    descricao           TEXT,
    data_tarefa           DATE NOT NULL,
    data_primeiro_aviso   DATE NOT NULL,
    data_segundo_aviso    DATE NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_tarefas_agendadas_condominio ON tarefas_agendadas (id_condominio);
