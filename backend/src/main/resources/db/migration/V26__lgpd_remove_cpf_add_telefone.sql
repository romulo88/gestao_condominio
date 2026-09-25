-- Adequação à LGPD (pedido do Romulo): CPF é dado pessoal sensível que o sistema não
-- precisa guardar - e-mail já identifica a Pessoa de sobra (login/"esqueci senha"/
-- cadastro passam a usar e-mail, não CPF - ver AuthService/FuncionarioService/
-- MoradorService). Telefone entra como novo dado de contato, opcional pros dois papéis.
ALTER TABLE pessoas DROP COLUMN cpf;
ALTER TABLE pessoas ADD COLUMN telefone TEXT;

-- E-mail passa a ser o identificador único de Pessoa (papel que o CPF tinha antes).
-- Índice parcial (não constraint direto na coluna) porque funcionário sem perfil pode não
-- ter e-mail - Postgres trata cada NULL como distinto, então múltiplos funcionários sem
-- e-mail não conflitam entre si.
CREATE UNIQUE INDEX pessoas_email_key ON pessoas (email) WHERE email IS NOT NULL;
