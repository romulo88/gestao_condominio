-- Item 4.8 de verdade: quem marcou a demanda como sigilosa (além de quem a abriu) também
-- ganha acesso automático e pode indicar outras pessoas - ver DemandaService/
-- DemandaAcessoSigilosoService. `demanda_acesso_sigiloso` (a tabela de quem mais tem
-- acesso) já existia desde o V1, sem uso até agora - essa migration só adiciona o campo
-- que faltava na própria demanda.
ALTER TABLE demandas
    ADD COLUMN id_funcionario_marcou_sigilo INTEGER REFERENCES funcionarios (id_funcionario);
