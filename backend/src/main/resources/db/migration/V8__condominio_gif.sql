-- GIF opcional do condomínio (pedido do Romulo) - exibido no lugar do título da página no
-- Kanban, quando cadastrado. Mesmo padrão de storage já usado pra foto de pessoa (V6) e
-- anexo de demanda: guarda a CHAVE do objeto no bucket S3-compatível (MinIO local /
-- Cloudflare R2 em produção), não uma URL pública - ver CondominioGifService.
ALTER TABLE condominios ADD COLUMN gif_url TEXT;
