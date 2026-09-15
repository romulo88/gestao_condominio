-- Foto de perfil da pessoa (pedido do Romulo pro cadastro de funcionário) - fica em
-- `pessoas`, não em `funcionarios`, porque só existe UMA foto por pessoa, mesmo que ela
-- tenha mais de um papel (funcionário e morador, por exemplo). Guarda a CHAVE do objeto
-- no bucket S3-compatível (MinIO local / Cloudflare R2 em produção), não uma URL pública
-- - mesmo padrão de demanda_documentos.url (ver DemandaDocumentoService/PessoaFotoService).
ALTER TABLE pessoas ADD COLUMN foto_url TEXT;
