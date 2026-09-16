-- Bootstrap: primeiro administrador do sistema. Sem isso, um deploy novo nao tem
-- ninguem que consiga logar pra cadastrar os demais usuarios pela propria aplicacao.
--
-- Senha ja definida e validada (precisa_trocar_senha = false) - pedido explicito de
-- pular o fluxo padrao de "toda pessoa nasce com a senha padrao Trocar@123" (ver
-- V4__senha_padrao_e_precisa_trocar.sql) so pra esse cadastro inicial. O hash bcrypt
-- (custo 10, mesmo custo do BCryptPasswordEncoder default da aplicacao - ver
-- SecurityConfig) foi gerado fora do banco; a senha em texto puro nunca fica em lugar
-- nenhum do codigo/schema.
INSERT INTO pessoas (nome, cpf, email, senha_hash, precisa_trocar_senha, created_at, updated_at)
VALUES (
    'Romulo Andrade',
    '04353921584',
    'romulo.eti@gmail.com',
    '$2y$10$RNceQ3jm.N.R9E/BeM/wJeG2H2NBIy4NduRLJRXE.MAiZl8yVYVDG',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO administradores (id_administrador, situacao, created_at, updated_at)
SELECT id_pessoa, 'ativo', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM pessoas
WHERE cpf = '04353921584';
