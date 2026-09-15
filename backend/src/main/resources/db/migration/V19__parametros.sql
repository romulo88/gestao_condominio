-- Parâmetros gerais do sistema (pedido do Romulo): valores de regra de negócio que hoje
-- são constante fixa no código (ex: limite de fotos por demanda) passam a morar aqui,
-- editáveis só pelo administrador, sem precisar subir versão nova do backend pra mudar o
-- VALOR. Formato livre (nome/valor em texto, não uma coluna por parâmetro) de propósito -
-- cadastrar um parâmetro novo continua exigindo código novo que leia esse nome (e,
-- portanto, uma migration pra semear a linha), mas editar o valor de um já existente é só
-- um PATCH.
CREATE TABLE parametros (
    id_parametro INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL UNIQUE,
    descricao    TEXT,
    valor        TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL
);

-- Semeia os 4 parâmetros que hoje são constante fixa em DemandaDocumentoService, com o
-- mesmo valor que já estava em uso (v146) - trocar a fonte do valor não muda o
-- comportamento atual pra quem já está usando o sistema.
INSERT INTO parametros (nome, descricao, valor, updated_at) VALUES
    ('maximoFotos', 'Máximo de fotos por demanda', '3', CURRENT_TIMESTAMP),
    ('maximoVideos', 'Máximo de vídeos por demanda', '1', CURRENT_TIMESTAMP),
    ('tamanhoMaximoFotoMb', 'Tamanho máximo (MB) de cada foto anexada numa demanda', '8', CURRENT_TIMESTAMP),
    ('tamanhoMaximoVideoMb', 'Tamanho máximo (MB) de cada vídeo anexado numa demanda', '15', CURRENT_TIMESTAMP);
