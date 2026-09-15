-- Primeira migration incremental do projeto - a partir daqui V1__init.sql NUNCA mais é
-- editado (o sistema já tem dado real de uso, editar um arquivo já aplicado quebra o
-- checksum que o Flyway guarda e ele recusa iniciar). Toda mudança de schema agora vira
-- um V(n)__ novo.
--
-- Dois ajustes no cluster de Demanda:
--   1) demandas.identificar_solicitante: se o nome de quem abriu aparece pro funcionário
--      no "Aberta por" da listagem - desmarcado por padrão pra morador (reclamação sem
--      se identificar pros outros funcionários), marcado por padrão pra funcionário
--      (default TRUE aqui é só o valor de segurança pro banco; quem decide o padrão por
--      papel é a tela de cadastro).
--   2) demanda_etapas.prazo: era TIMESTAMP NOT NULL, virou DATE opcional - só data
--      (sem hora) e nem toda etapa precisa de prazo marcado.

ALTER TABLE demandas
    ADD COLUMN identificar_solicitante BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE demanda_etapas
    ALTER COLUMN prazo TYPE DATE USING prazo::date,
    ALTER COLUMN prazo DROP NOT NULL;
