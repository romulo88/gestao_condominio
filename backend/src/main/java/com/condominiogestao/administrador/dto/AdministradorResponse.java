package com.condominiogestao.administrador.dto;

import com.condominiogestao.administrador.Administrador;
import com.condominiogestao.common.Situacao;
import java.time.LocalDateTime;

/** Nunca expõe {@code senhaHash}. */
public record AdministradorResponse(
        Integer id,
        String nome,
        String cpf,
        String email,
        Situacao situacao,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AdministradorResponse from(Administrador administrador) {
        return new AdministradorResponse(
                administrador.getId(),
                administrador.getNome(),
                administrador.getCpf(),
                administrador.getEmail(),
                administrador.getSituacao(),
                administrador.getCreatedAt(),
                administrador.getUpdatedAt());
    }
}
