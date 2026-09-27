-- Vincula uma demanda à ronda durante a qual foi aberta (pedido do Romulo, feature
-- "Controle de Rondas") - nullable, só demandas criadas a partir da tela de ronda têm
-- valor. Não muda em nada o fluxo de aprovação da demanda em si.
ALTER TABLE demandas ADD COLUMN id_ronda INTEGER REFERENCES rondas (id_ronda);
