-- Feature "Controle de Rondas" (pedido do Romulo): registra o que um rondista fez no
-- turno - início/fim, trajeto (pontos de GPS) e demandas abertas durante a ronda. A ronda
-- é "livre" (sem pontos de controle nem rota cadastrada), então o sistema só grava o que
-- de fato aconteceu.
--
-- `status` segue o padrão enum-via-CHECK do projeto (ver V1__init.sql) em vez de enum
-- nativo do Postgres. Três jeitos de uma ronda `em_andamento` deixar de estar ativa (ver
-- RondaService): o próprio rondista finaliza (`finalizada`), um perfil completo (síndico/
-- sub-síndico/encarregado/supervisor) encerra uma ronda esquecida de outro rondista
-- (`encerrada_manualmente`), ou ninguém fecha em 12h e o sistema encerra sozinho na
-- próxima leitura que tocar essa ronda (`encerrada_automaticamente`) - sem job agendado,
-- o projeto não tem nenhum @Scheduled hoje.
CREATE TABLE rondas (
    id_ronda INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio INTEGER NOT NULL REFERENCES condominios (id_condominio),
    id_funcionario INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    iniciada_em TIMESTAMP NOT NULL,
    finalizada_em TIMESTAMP,
    status VARCHAR(30) NOT NULL CHECK (status IN (
        'em_andamento', 'finalizada', 'encerrada_manualmente', 'encerrada_automaticamente'
    )),
    distancia_metros DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rondas_condominio_iniciada_em ON rondas (id_condominio, iniciada_em DESC);

-- Pontos de GPS gravados durante a ronda (celular no suporte do guidão da moto) - enviados
-- em lote pelo cliente a cada ~20s, não um a um, pra tolerar sinal ruim.
CREATE TABLE ronda_pontos (
    id_ponto INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_ronda INTEGER NOT NULL REFERENCES rondas (id_ronda),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    capturado_em TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ronda_pontos_ronda_capturado_em ON ronda_pontos (id_ronda, capturado_em);
