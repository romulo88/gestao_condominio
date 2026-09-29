-- Espaços de lazer do condomínio (pedido do Romulo: aba nova em Condomínio, porque cada
-- condomínio tem um conjunto diferente - hoje um tem 5 quiosques + 1 salão gourmet + 1
-- salão de festas, outro pode ter outra coisa). Usado como opção de "local" ao cadastrar
-- um Evento (ver V34) - mesmo padrão de Etiqueta (nome + situação ativo/inativo).
CREATE TABLE espacos_comuns (
    id_espaco_comum INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio   INTEGER NOT NULL REFERENCES condominios (id_condominio),
    nome            VARCHAR(50) NOT NULL,
    situacao        VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_condominio, nome)
);
