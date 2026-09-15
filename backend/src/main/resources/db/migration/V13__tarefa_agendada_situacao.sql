-- Pedido do Romulo: "na lista de tarefas agendadas, colocar editar e remover". A V10
-- deixou isso deliberadamente de fora ("sem editar/concluir/remover... se precisar, entra
-- depois, no mesmo espírito soft-delete do resto do sistema") - esta migration cumpre essa
-- promessa. "Remover" na aplicação é soft-delete (situacao = inativo), mesmo padrão de
-- Etiqueta/Aviso/MensagemRapida - a listagem passa a mostrar só as ativas.
ALTER TABLE tarefas_agendadas ADD COLUMN situacao VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo'));
