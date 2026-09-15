-- Senha padrão + troca obrigatória: toda Pessoa nasce com uma senha conhecida
-- ("Trocar@123"), mas nunca serve pra logar de verdade - só pra abrir o fluxo de
-- "Esqueci minha senha" (POST /api/auth/verificar-identidade + /api/auth/trocar-senha),
-- que troca a senha e desliga essa flag. Substitui de vez o antigo /api/auth/definir-senha
-- (provisório, nunca chamado pelo frontend).
ALTER TABLE pessoas ADD COLUMN precisa_trocar_senha BOOLEAN NOT NULL DEFAULT false;

-- Pessoa que nunca teve senha nenhuma (senha_hash null) ganha a senha padrão e é
-- marcada como "precisa trocar" - hash real de "Trocar@123", gerado com o mesmo
-- BCryptPasswordEncoder que a aplicação usa (não um valor inventado).
UPDATE pessoas
SET senha_hash = '$2a$10$hnLeLHfJVTl4VGIsSgakg.KXe1rT/QDFAzV4ulH7ljenyvtiihZ1S',
    precisa_trocar_senha = true
WHERE senha_hash IS NULL;
