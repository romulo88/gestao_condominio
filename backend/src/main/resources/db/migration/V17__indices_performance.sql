-- Índices de performance. O Postgres cria índice automático só para PK e UNIQUE - nunca
-- para chave estrangeira. Até aqui o schema tinha 4 índices explícitos (V10, V12, V14,
-- V16), então as tabelas mais consultadas do sistema (demandas e suas filhas) faziam
-- seq scan a cada carregamento do Kanban.
--
-- Só entram aqui as colunas que alguma consulta real usa e que nenhum UNIQUE/PK já cobre.
-- Deliberadamente FORA (já cobertos pelo prefixo de um índice existente):
--   demanda_responsaveis   -> UNIQUE (id_demanda, id_funcionario)
--   demanda_etiquetas      -> PK (id_demanda, id_etiqueta)
--   demanda_acesso_sigiloso-> UNIQUE (id_demanda, tipo_pessoa, ...)
--   demanda_acompanhamentos-> UNIQUE (id_demanda, id_morador)
--   blocos / status_kanban / etiquetas -> UNIQUE (id_condominio, nome|descricao)
--   funcionarios_condominios por funcionário -> UNIQUE (id_funcionario, id_condominio)
--   demanda_notas          -> idx_demanda_notas_demanda, criado no V14

-- demandas: nenhuma FK indexada. Composto porque DemandaService.listar sempre quer as
-- demandas de um condomínio em ordem decrescente de criação (hoje ordenadas em memória;
-- o índice já deixa pronto para trocar por findByCondominioIdOrderByCreatedAtDesc).
CREATE INDEX idx_demandas_condominio_created_at
    ON demandas (id_condominio, created_at DESC);

-- DemandaRepository.findByMoradorSolicitanteIdAndDataAprovacaoAfter - roda a cada login
-- de morador (alerta de mudanças de status).
CREATE INDEX idx_demandas_morador_data_aprovacao
    ON demandas (id_morador_solicitante, data_aprovacao);

-- Filhas de demanda, todas carregadas em lote (findByDemandaIdIn) em toda listagem.
-- Compostas com a coluna de ordenação usada pelo repository, para evitar sort.
CREATE INDEX idx_demanda_etapas_demanda_ordem
    ON demanda_etapas (id_demanda, ordem);

CREATE INDEX idx_demanda_documentos_demanda
    ON demanda_documentos (id_demanda);

CREATE INDEX idx_demanda_hist_kanban_demanda_created_at
    ON demanda_status_kanban_historico (id_demanda, created_at);

-- Vínculos de morador: esta é a única tabela de vínculo sem UNIQUE nenhum, então não há
-- índice em nada além da PK sintética. Usada em findByMoradorId (caminho de login),
-- findByCondominioId e countAtivosPorCondominio.
CREATE INDEX idx_moradores_condominios_morador_condominio
    ON moradores_condominios (id_morador, id_condominio);

CREATE INDEX idx_moradores_condominios_condominio
    ON moradores_condominios (id_condominio);

-- O UNIQUE de funcionarios_condominios é (id_funcionario, id_condominio): serve para
-- buscar por funcionário, mas não por condomínio isolado (findByCondominioId /
-- countAtivosPorCondominio, usados na listagem de condomínios e no roster do Kanban).
CREATE INDEX idx_funcionarios_condominios_condominio
    ON funcionarios_condominios (id_condominio);

-- AvisoRepository.findByCondominioIdOrderByCreatedAtDesc / findVisiveisPorCondominio.
CREATE INDEX idx_avisos_condominio_created_at
    ON avisos (id_condominio, created_at DESC);
