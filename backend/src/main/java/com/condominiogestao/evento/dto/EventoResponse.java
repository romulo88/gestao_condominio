package com.condominiogestao.evento.dto;

import com.condominiogestao.evento.Evento;
import com.condominiogestao.evento.EventoPessoa;
import com.condominiogestao.evento.EventoVeiculo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EventoResponse(
        Integer id,
        Integer condominioId,
        Integer espacoComumId,
        /** Null quando o local é a própria unidade do morador. */
        String espacoComumNome,
        String motivo,
        LocalDate data,
        String horario,
        String moradorNome,
        /** Número do apto/casa (vínculo do morador nesse condomínio) - null se não achar. */
        String unidade,
        List<EventoVeiculoResponse> veiculos,
        List<EventoPessoaResponse> pessoas,
        LocalDateTime createdAt) {

    public static EventoResponse from(
            Evento evento, String unidade, List<EventoVeiculo> veiculos, List<EventoPessoa> pessoas) {
        return new EventoResponse(
                evento.getId(),
                evento.getCondominio().getId(),
                evento.getEspacoComum() == null ? null : evento.getEspacoComum().getId(),
                evento.getEspacoComum() == null ? null : evento.getEspacoComum().getNome(),
                evento.getMotivo(),
                evento.getData(),
                evento.getHorario(),
                evento.getMorador().getNome(),
                unidade,
                veiculos.stream().map(EventoVeiculoResponse::from).toList(),
                pessoas.stream().map(EventoPessoaResponse::from).toList(),
                evento.getCreatedAt());
    }
}
