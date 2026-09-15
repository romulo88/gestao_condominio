-- Pedido do Romulo: "deixar o kanban disponível em um link externo, independente do
-- usuário estar logado" - clicando num ícone na grid de Condomínios, o sistema gera um
-- link público (token opaco, sem relação com nenhum outro id) que abre uma versão
-- somente-leitura do quadro Kanban, sem exigir login. Nulo = condomínio nunca gerou (ou
-- revogou) o link público - UNIQUE permite múltiplos NULL no Postgres, sem problema.
ALTER TABLE condominios ADD COLUMN kanban_publico_token TEXT UNIQUE;
