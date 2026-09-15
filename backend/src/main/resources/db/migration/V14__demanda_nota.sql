-- Notas numa demanda (pedido do Romulo): depois que a demanda é cadastrada, o morador pode
-- perguntar sobre o andamento e o funcionário pode responder com outra nota, se julgar
-- necessário. `id_nota_pai` aponta pra outra nota da MESMA demanda, formando a resposta -
-- é o que permite a listagem vir indentada (nota respondida fica indentada em relação à
-- nota original). Autor é sempre exatamente um entre morador OU funcionário, mesmo padrão
-- do solicitante da demanda (chk_demanda_solicitante_unico, V1).
--
-- "lida"/"lida_em" só é setado por funcionário - manual (ícone na listagem) ou automático
-- (ao responder). Regra "notas só podem ser cadastradas até a demanda ser arquivada" é
-- checada em tempo de execução no service (não dá pra expressar com CHECK, depende da
-- tabela demandas).
CREATE TABLE demanda_notas (
    id_nota        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_demanda     INTEGER NOT NULL REFERENCES demandas (id_demanda),
    id_nota_pai    INTEGER REFERENCES demanda_notas (id_nota),
    id_morador     INTEGER REFERENCES moradores (id_morador),
    id_funcionario INTEGER REFERENCES funcionarios (id_funcionario),
    texto          VARCHAR(150) NOT NULL,
    lida           BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em        TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_demanda_nota_autor_unico CHECK (
        (id_morador IS NOT NULL) <> (id_funcionario IS NOT NULL)
    )
);

CREATE INDEX idx_demanda_notas_demanda ON demanda_notas (id_demanda);
