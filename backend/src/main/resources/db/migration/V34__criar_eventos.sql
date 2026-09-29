-- Cadastro de eventos pelo morador (pedido do Romulo: hoje ele avisa a portaria de festas/
-- visitas por WhatsApp, sem histórico nem padronização). `id_espaco_comum` nullable = local
-- é a própria unidade do morador; preenchido = um dos espaços de lazer (ver V33). Não é
-- sistema de reserva - dois eventos no mesmo espaço no mesmo dia não geram conflito, é só
-- um registro pra portaria consultar.
CREATE TABLE eventos (
    id_evento       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio   INTEGER NOT NULL REFERENCES condominios (id_condominio),
    id_morador      INTEGER NOT NULL REFERENCES moradores (id_morador),
    id_espaco_comum INTEGER REFERENCES espacos_comuns (id_espaco_comum),
    motivo          VARCHAR(200) NOT NULL,
    data            DATE NOT NULL,
    horario         VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_eventos_condominio_data ON eventos (id_condominio, data);

-- Liberação por item (pessoa/veículo), não por evento inteiro - pedido do Romulo: "as
-- pessoas chegam em horários diferentes". `liberado` é um toggle (liberar de novo desfaz -
-- corrige engano do porteiro sem endpoint separado); `id_funcionario_liberou`/`liberado_em`
-- só ficam preenchidos enquanto `liberado = true`.
CREATE TABLE evento_veiculos (
    id_evento_veiculo    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento            INTEGER NOT NULL REFERENCES eventos (id_evento),
    placa                VARCHAR(8) NOT NULL,
    liberado             BOOLEAN NOT NULL DEFAULT false,
    id_funcionario_liberou INTEGER REFERENCES funcionarios (id_funcionario),
    liberado_em          TIMESTAMP
);
CREATE INDEX idx_evento_veiculos_evento ON evento_veiculos (id_evento);

CREATE TABLE evento_pessoas (
    id_evento_pessoa     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_evento            INTEGER NOT NULL REFERENCES eventos (id_evento),
    nome                 VARCHAR(120) NOT NULL,
    documento            VARCHAR(50) NOT NULL,
    liberado             BOOLEAN NOT NULL DEFAULT false,
    id_funcionario_liberou INTEGER REFERENCES funcionarios (id_funcionario),
    liberado_em          TIMESTAMP
);
CREATE INDEX idx_evento_pessoas_evento ON evento_pessoas (id_evento);
