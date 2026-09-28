-- Tipo da ronda + observação do rondista (pedido do Romulo): informados pelo rondista na
-- hora de finalizar (modal do botão "Finalizar ronda"). "normal" = ronda em volta do
-- condomínio, rota combinada antes com o síndico; "acompanhamento" = rondista acompanha um
-- entregador/caminhão de mudança até a casa do morador e garante que ele saia do condomínio.
--
-- Nullable: uma ronda em andamento ainda não tem tipo, e ronda encerrada por outra pessoa
-- (perfil completo, `encerrada_manualmente`) ou pelo sistema (`encerrada_automaticamente`)
-- fica sem tipo - só o próprio rondista sabe o que fez. Rondas anteriores a esta migration
-- também ficam null.
--
-- Mesmo padrão enum-via-CHECK do projeto (ver V1/V28/V29) - lembrar de atualizar a CHECK
-- junto com o enum Java `TipoRonda` se um valor novo entrar.
ALTER TABLE rondas ADD COLUMN tipo VARCHAR(30) CHECK (tipo IN ('normal', 'acompanhamento'));
ALTER TABLE rondas ADD COLUMN observacao VARCHAR(100);
