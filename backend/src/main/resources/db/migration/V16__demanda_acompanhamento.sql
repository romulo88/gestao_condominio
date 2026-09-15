-- Pedido do Romulo: funcionalidade "Acompanhar" - morador marca um check numa demanda
-- que ele não abriu pra receber as mudanças de status dela junto do alerta de login
-- (ver DemandaService.mudancasStatus). Guarda só o id da demanda e o id do morador que
-- decidiu acompanhar (não "pessoa" genérica - a funcionalidade é morador-only, mesmo
-- padrão de FK que o resto do schema usa pra relação role-specific em vez de Pessoa
-- direto, ex: demanda_notas, demanda_documentos, demanda_acesso_sigiloso).
CREATE TABLE demanda_acompanhamentos (
    id_acompanhamento SERIAL PRIMARY KEY,
    id_demanda INTEGER NOT NULL REFERENCES demandas(id_demanda),
    id_morador INTEGER NOT NULL REFERENCES moradores(id_morador),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_demanda_acompanhamento UNIQUE (id_demanda, id_morador)
);

CREATE INDEX idx_demanda_acompanhamentos_morador ON demanda_acompanhamentos (id_morador);
