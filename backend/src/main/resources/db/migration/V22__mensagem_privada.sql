-- Mensagem privada (pedido do Romulo): diferente de demanda_notas (atrelada a uma demanda,
-- visível pra quem enxerga a demanda), aqui é uma conversa independente, criada por morador
-- OU funcionário, endereçada a um ou mais funcionários COM LOGIN (perfil preenchido) do
-- mesmo condomínio - só o autor e os destinatários enxergam, ninguém mais (nem síndico por
-- regra, como já acontece no sigilo de demanda - decidido explicitamente diferente aqui,
-- pedido é privacidade de verdade). Virou "conversa livre" (várias mensagens de ida e volta,
-- não só pergunta+resposta) - por isso duas tabelas: conversas_privadas é o continer (quem
-- participa, quando cada um viu por último) e mensagens_privadas é cada linha dentro dela.
--
-- Autor da conversa (quem iniciou) é sempre exatamente um entre morador OU funcionário,
-- mesmo padrão de chk_demanda_solicitante_unico (V1)/chk_demanda_nota_autor_unico (V14).
-- Autor de cada MENSAGEM dentro da conversa segue a mesma regra - pode ser o autor
-- original ou qualquer um dos destinatários (todo mundo na conversa pode escrever).
CREATE TABLE conversas_privadas (
    id_conversa                  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio                INTEGER NOT NULL REFERENCES condominios (id_condominio),
    id_morador_autor             INTEGER REFERENCES moradores (id_morador),
    id_funcionario_autor         INTEGER REFERENCES funcionarios (id_funcionario),
    -- Marca quando o AUTOR viu a conversa por último - junto com a mesma coluna em
    -- conversas_privadas_destinatarios (por destinatário), dá pra saber quem tem mensagem
    -- não vista sem precisar de um flag "lida" por mensagem individual.
    autor_ultima_visualizacao_em TIMESTAMP,
    created_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_conversa_privada_autor_unico CHECK (
        (id_morador_autor IS NOT NULL) <> (id_funcionario_autor IS NOT NULL)
    )
);

CREATE INDEX idx_conversas_privadas_condominio ON conversas_privadas (id_condominio);

-- Destinatários (N:N) - uma conversa pode ter mais de um funcionário endereçado (ex:
-- síndico E sub-síndico ao mesmo tempo, pedido do Romulo). ultima_visualizacao_em é POR
-- destinatário - se o síndico já viu mas o sub-síndico não, cada um mantém o próprio estado.
CREATE TABLE conversas_privadas_destinatarios (
    id_destinatario         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conversa              INTEGER NOT NULL REFERENCES conversas_privadas (id_conversa),
    id_funcionario           INTEGER NOT NULL REFERENCES funcionarios (id_funcionario),
    ultima_visualizacao_em   TIMESTAMP,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_conversa_privada_destinatario UNIQUE (id_conversa, id_funcionario)
);

CREATE INDEX idx_conversas_privadas_destinatarios_conversa ON conversas_privadas_destinatarios (id_conversa);
CREATE INDEX idx_conversas_privadas_destinatarios_funcionario ON conversas_privadas_destinatarios (id_funcionario);

-- Cada linha da conversa (a "mensagem" que o Romulo pediu, no sentido chat mesmo). Sem
-- limite de caracteres de propósito (diferente de demanda_notas/mensagens_rapidas, que são
-- VARCHAR(150)) - aqui é uma conversa de verdade, não uma pergunta curta.
CREATE TABLE mensagens_privadas (
    id_mensagem     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_conversa     INTEGER NOT NULL REFERENCES conversas_privadas (id_conversa),
    id_morador_autor     INTEGER REFERENCES moradores (id_morador),
    id_funcionario_autor INTEGER REFERENCES funcionarios (id_funcionario),
    texto           TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mensagem_privada_autor_unico CHECK (
        (id_morador_autor IS NOT NULL) <> (id_funcionario_autor IS NOT NULL)
    )
);

CREATE INDEX idx_mensagens_privadas_conversa ON mensagens_privadas (id_conversa);

-- 1 foto por mensagem por padrão (pedido do Romulo, quantidade parametrizável - ver
-- ParametroService/parametros.nome = 'mensagemPrivadaMaximoFotos', mesmo mecanismo já
-- usado pra foto/vídeo de demanda, V19). Mesmo formato de demanda_documentos.
CREATE TABLE mensagens_privadas_documentos (
    id_documento    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_mensagem     INTEGER NOT NULL REFERENCES mensagens_privadas (id_mensagem),
    nome_arquivo    TEXT,
    url             TEXT,
    tipo_mime       TEXT,
    tamanho_bytes   INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensagens_privadas_documentos_mensagem ON mensagens_privadas_documentos (id_mensagem);
