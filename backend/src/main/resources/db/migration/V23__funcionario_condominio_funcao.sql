-- Função do funcionário (pedido do Romulo): pra quem não tem perfil (sem acesso ao
-- sistema nesse condomínio - ver `funcionarios_condominios.perfil`, V1), não existe hoje
-- nenhum campo que diga o que essa pessoa faz (jardineiro, rondista, etc.). Texto livre,
-- opcional, POR vínculo (mesmo espírito de `perfil`/`numero_unidade` de morador) - o
-- mesmo funcionário pode ter uma função diferente em cada condomínio.
ALTER TABLE funcionarios_condominios ADD COLUMN funcao TEXT;
