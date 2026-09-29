-- Perfil de acesso restrito novo (Porteiro, pedido do Romulo) - mesma lição documentada na
-- V28: a constraint CHECK de `perfil` precisa listar o valor novo, senão trava no banco
-- mesmo com o código já aceitando (403 vazio, sintoma de DataIntegrityViolation).
ALTER TABLE funcionarios_condominios DROP CONSTRAINT funcionarios_condominios_perfil_check;
ALTER TABLE funcionarios_condominios ADD CONSTRAINT funcionarios_condominios_perfil_check
    CHECK (perfil::text = ANY (ARRAY[
        'sindico', 'sub_sindico', 'supervisor', 'encarregado', 'rondista', 'agente_convivio', 'porteiro'
    ]::text[]));
