package com.condominiogestao.evento.dto;

import com.condominiogestao.evento.EventoVeiculo;
import java.time.LocalDateTime;

public record EventoVeiculoResponse(
        Integer id, String placa, boolean liberado, String liberadoPorNome, LocalDateTime liberadoEm) {

    public static EventoVeiculoResponse from(EventoVeiculo veiculo) {
        return new EventoVeiculoResponse(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.isLiberado(),
                veiculo.getFuncionarioLiberou() == null ? null : veiculo.getFuncionarioLiberou().getNome(),
                veiculo.getLiberadoEm());
    }
}
