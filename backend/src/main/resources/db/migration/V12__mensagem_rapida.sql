-- Cadastro de mensagens rápidas (pedido do Romulo): texto pronto por condomínio, pra
-- funcionário reaproveitar em vez de digitar do zero toda vez (ex: respostas padrão pra
-- demanda). Cada mensagem tem um caráter - positivo ou negativo - pra dar pra separar
-- rapidinho na hora de escolher (ex: positiva = "Obrigado pelo contato, já resolvemos!",
-- negativa = "Infelizmente não vamos conseguir atender esse pedido."). Mesmo padrão
-- soft-delete de Etiqueta/Aviso: "excluir" na aplicação é situacao = inativo, sem apagar
-- nada do banco - a listagem só mostra as ativas.
CREATE TABLE mensagens_rapidas (
    id_mensagem_rapida INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_condominio       INTEGER NOT NULL REFERENCES condominios (id_condominio),
    texto                VARCHAR(150) NOT NULL,
    carater              VARCHAR(30) NOT NULL CHECK (carater IN ('positivo', 'negativo')),
    situacao             VARCHAR(30) NOT NULL DEFAULT 'ativo' CHECK (situacao IN ('ativo', 'inativo')),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL
);

CREATE INDEX idx_mensagens_rapidas_condominio ON mensagens_rapidas (id_condominio);
