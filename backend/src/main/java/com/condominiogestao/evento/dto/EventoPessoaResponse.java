package com.condominiogestao.evento.dto;

import com.condominiogestao.evento.EventoPessoa;
import java.time.LocalDateTime;

/** {@code fotoUrl} só vem preenchido pra quem pode ver (porteiro/perfil completo - nunca
 * o morador, mesmo o dono do evento) - ver {@code Autorizacao.ehPorteiroOuPerfilCompleto}
 * e {@code EventoService}. */
public record EventoPessoaResponse(
        Integer id,
        String nome,
        String documento,
        boolean liberado,
        String liberadoPorNome,
        LocalDateTime liberadoEm,
        String fotoUrl) {

    public static EventoPessoaResponse from(EventoPessoa pessoa, boolean podeVerFoto) {
        return new EventoPessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getDocumento(),
                pessoa.isLiberado(),
                pessoa.getFuncionarioLiberou() == null ? null : pessoa.getFuncionarioLiberou().getNome(),
                pessoa.getLiberadoEm(),
                podeVerFoto && pessoa.getFotoChave() != null
                        ? "/api/eventos/%d/pessoas/%d/foto".formatted(pessoa.getEvento().getId(), pessoa.getId())
                        : null);
    }
}
