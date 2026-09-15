-- Schema inicial - Sistema de Gestão de Demandas de Condomínios
-- Equivalente ao antigo prisma/schema.prisma (Node/Prisma), agora sob o Flyway (Java/Spring).
--
-- Diferença deliberada em relação à versão Prisma: os "enums" viram VARCHAR + CHECK
-- em vez de tipos ENUM nativos do Postgres. Hibernate/JPA tem atrito conhecido com
-- tipos enum nativos (exige @JdbcTypeCode extra); VARCHAR+CHECK dá a mesma garantia de
-- integridade no banco com @Enumerated(EnumType.STRING) simples do lado Java.

-- ---------------------------------------------------------------------------
-- CONDOMÍNIO / ESTRUTURA FÍSICA
-- ---------------------------------------------------------------------------

CREATE TABLE condominios (
    id_condominio    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome             TEXT NOT NULL,
    cnpj             TEXT NOT NULL UNIQUE,
    tipo             VARCHAR(30) NOT NULL CHECK (tipo IN ('apartamento', 'casas')),
    situacao         VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    quantidade_casas INTEGER,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL
);

CREATE TABLE blocos (
    id_bloco      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio INTEGER NOT NULL REFERENCES condominios (id_condominio),
    nome          TEXT NOT NULL,
    UNIQUE (id_condominio, nome)
);

-- ---------------------------------------------------------------------------
-- CONFIGURAÇÃO POR CONDOMÍNIO
-- ---------------------------------------------------------------------------

CREATE TABLE status_kanban (
    id_status_kanban INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio    INTEGER NOT NULL REFERENCES condominios (id_condominio),
    nome             TEXT NOT NULL,
    ordem            INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL,
    UNIQUE (id_condominio, nome)
);

CREATE TABLE etiquetas (
    id_etiqueta   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio INTEGER NOT NULL REFERENCES condominios (id_condominio),
    descricao     VARCHAR(50) NOT NULL,
    cor           TEXT NOT NULL,
    situacao      VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL,
    UNIQUE (id_condominio, descricao)
);

-- ---------------------------------------------------------------------------
-- PESSOAS
-- ---------------------------------------------------------------------------

-- Identidade compartilhada: nome, cpf (login), email, senha. Funcionario e Morador são
-- extensões 1:1 dessa tabela (mesma PK, ver abaixo) - a mesma pessoa pode ter os dois
-- papéis ao mesmo tempo (ex: síndico que também mora no condomínio).
CREATE TABLE pessoas (
    id_pessoa   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        TEXT NOT NULL,
    cpf         TEXT NOT NULL UNIQUE,
    email       TEXT,
    senha_hash  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL
);

-- id_funcionario É id_pessoa (chave primária compartilhada, não gerada aqui) - por isso
-- não é IDENTITY: o valor vem sempre de uma linha já existente em pessoas.
CREATE TABLE funcionarios (
    id_funcionario INTEGER PRIMARY KEY REFERENCES pessoas (id_pessoa),
    situacao       VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL
);

CREATE TABLE funcionarios_condominios (
    id_vinculo     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_funcionario INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    id_condominio  INTEGER NOT NULL REFERENCES condominios (id_condominio),
    perfil         VARCHAR(30) CHECK (perfil IN ('sindico', 'sub_sindico', 'supervisor', 'encarregado')),
    situacao       VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL,
    UNIQUE (id_funcionario, id_condominio)
);

-- id_morador É id_pessoa (chave primária compartilhada) - mesmo esquema de funcionarios.
CREATE TABLE moradores (
    id_morador  INTEGER PRIMARY KEY REFERENCES pessoas (id_pessoa),
    situacao    VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL
);

CREATE TABLE moradores_condominios (
    id_vinculo      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_morador      INTEGER NOT NULL REFERENCES moradores (id_morador),
    id_condominio   INTEGER NOT NULL REFERENCES condominios (id_condominio),
    id_bloco        INTEGER REFERENCES blocos (id_bloco),
    numero_unidade  TEXT NOT NULL,
    situacao        VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL
);

-- Papel GLOBAL, sem vínculo com nenhum condomínio específico (por isso não existe uma
-- "administradores_condominios" - diferente de funcionário/morador, que são sempre
-- ligados a um ou mais condomínios). É quem opera o sistema como um todo (cadastra
-- condomínios, etc.), não quem administra um prédio específico (isso é o síndico).
-- id_administrador É id_pessoa - mesmo esquema de funcionarios/moradores.
CREATE TABLE administradores (
    id_administrador  INTEGER PRIMARY KEY REFERENCES pessoas (id_pessoa),
    situacao          VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL
);

-- ---------------------------------------------------------------------------
-- QUADRO DE AVISOS
-- ---------------------------------------------------------------------------

-- Comunicado geral do condomínio, mantido pelos funcionários (ex: "Piscina interditada
-- devido a vazamentos") - reduz demandas desnecessárias por algo que já é sabido.
-- Visível quando situacao = ativo E (data_expiracao é nula OU ainda não passou).
CREATE TABLE avisos (
    id_aviso       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio  INTEGER NOT NULL REFERENCES condominios (id_condominio),
    id_funcionario INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    descricao      VARCHAR(250) NOT NULL,
    situacao       VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    data_expiracao TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL
);

-- ---------------------------------------------------------------------------
-- DEMANDAS
-- ---------------------------------------------------------------------------

CREATE TABLE demandas (
    id_demanda                   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio                INTEGER NOT NULL REFERENCES condominios (id_condominio),
    titulo                       TEXT NOT NULL,
    descricao                    TEXT NOT NULL,
    id_morador_solicitante       INTEGER REFERENCES moradores (id_morador),
    id_funcionario_solicitante   INTEGER REFERENCES funcionarios (id_funcionario),
    status_aprovacao             VARCHAR(30) NOT NULL DEFAULT 'pendente'
                                     CHECK (status_aprovacao IN ('pendente', 'aprovada', 'reprovada')),
    id_funcionario_aprovador     INTEGER REFERENCES funcionarios (id_funcionario),
    data_aprovacao               TIMESTAMP,
    justificativa_reprovacao     TEXT,
    id_status_kanban             INTEGER REFERENCES status_kanban (id_status_kanban),
    id_funcionario_responsavel   INTEGER REFERENCES funcionarios (id_funcionario),
    sigilosa                     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP NOT NULL,
    -- Origem: exatamente um entre morador/funcionário solicitante (nunca os dois, nunca nenhum)
    CONSTRAINT chk_demanda_solicitante_unico CHECK (
        (id_morador_solicitante IS NOT NULL) <> (id_funcionario_solicitante IS NOT NULL)
    )
);

CREATE TABLE demanda_etapas (
    id_etapa     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demanda   INTEGER NOT NULL REFERENCES demandas (id_demanda),
    nome         TEXT NOT NULL,
    prazo        TIMESTAMP NOT NULL,
    concluida    BOOLEAN NOT NULL DEFAULT FALSE,
    concluida_em TIMESTAMP,
    ordem        INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE demanda_documentos (
    id_documento          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demanda            INTEGER NOT NULL REFERENCES demandas (id_demanda),
    nome_arquivo          TEXT NOT NULL,
    url                   TEXT NOT NULL,
    tipo_mime             TEXT NOT NULL,
    tamanho_bytes         INTEGER NOT NULL,
    id_morador_upload     INTEGER REFERENCES moradores (id_morador),
    id_funcionario_upload INTEGER REFERENCES funcionarios (id_funcionario),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE demanda_status_kanban_historico (
    id_historico       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demanda         INTEGER NOT NULL REFERENCES demandas (id_demanda),
    id_status_anterior INTEGER REFERENCES status_kanban (id_status_kanban),
    id_status_novo     INTEGER NOT NULL REFERENCES status_kanban (id_status_kanban),
    id_funcionario     INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE demanda_etiquetas (
    id_demanda  INTEGER NOT NULL REFERENCES demandas (id_demanda),
    id_etiqueta INTEGER NOT NULL REFERENCES etiquetas (id_etiqueta),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_demanda, id_etiqueta)
);

CREATE TABLE demanda_acesso_sigiloso (
    id_acesso      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demanda     INTEGER NOT NULL REFERENCES demandas (id_demanda),
    tipo_pessoa    VARCHAR(30) NOT NULL CHECK (tipo_pessoa IN ('funcionario', 'morador')),
    id_morador     INTEGER REFERENCES moradores (id_morador),
    id_funcionario INTEGER REFERENCES funcionarios (id_funcionario),
    UNIQUE (id_demanda, tipo_pessoa, id_morador, id_funcionario)
);
