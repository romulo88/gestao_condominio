-- Termo de responsabilidade (pedido do Romulo): toda pessoa precisa aceitar, no primeiro
-- login, um aviso de que é responsável pelos dados que acessa no sistema (não fotografar/
-- copiar/repassar informação sem autorização) - o condomínio/sistema não responde por
-- divulgação indevida feita por um usuário.
--
-- termos_versao_aceita guarda a versão do texto que a pessoa aceitou (comparada contra a
-- constante TermosResponsabilidade.VERSAO_ATUAL no código - nunca guardada em Parametro,
-- de propósito: mudar o texto exige revisão/deploy, não uma edição solta em tela de admin).
-- null = nunca aceitou nenhuma versão (pessoa nova, ou pessoa de antes dessa migration).
-- Quando o texto mudar, a constante sobe (ex: 1 -> 2) e todo mundo com versão menor volta
-- a ver o aviso no próximo login - sem precisar de UPDATE em massa aqui.
ALTER TABLE pessoas ADD COLUMN termos_versao_aceita INTEGER;
ALTER TABLE pessoas ADD COLUMN termos_aceitos_em TIMESTAMP;
