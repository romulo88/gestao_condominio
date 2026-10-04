-- `eventos.id_morador` é filtrado em toda abertura de "Meus eventos" (EventoRepository.
-- findByMoradorIdOrderByDataDesc) e no autocomplete de visitante/veículo recorrente
-- (EventoPessoaRepository/EventoVeiculoRepository.findTop100ByEvento_Morador_IdOrderByIdDesc,
-- chamado a cada criação de evento) - só havia índice em (id_condominio, data) (ver V34),
-- sem `id_morador` como prefixo. Sem isso, cada uma dessas consultas faz seq scan em
-- `eventos` conforme o condomínio acumula eventos (reforma cria até 15 de uma vez - V38).
CREATE INDEX idx_eventos_morador ON eventos (id_morador, data DESC);
