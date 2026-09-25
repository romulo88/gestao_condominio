-- Perfis de acesso restrito (Rondista/Agente de Convívio, pedido do Romulo) - a constraint
-- CHECK de `perfil` só permitia os 4 valores antigos, travando o enum novo no banco mesmo
-- com o código já aceitando (achado ao testar: 403 vazio, sintoma de DataIntegrityViolation
-- na constraint, não de autorização).
ALTER TABLE funcionarios_condominios DROP CONSTRAINT funcionarios_condominios_perfil_check;
ALTER TABLE funcionarios_condominios ADD CONSTRAINT funcionarios_condominios_perfil_check
    CHECK (perfil::text = ANY (ARRAY[
        'sindico', 'sub_sindico', 'supervisor', 'encarregado', 'rondista', 'agente_convivio'
    ]::text[]));
