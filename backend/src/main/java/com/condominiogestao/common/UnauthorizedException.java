package com.condominiogestao.common;

/** Credenciais inválidas, token inválido/expirado, ou vínculo inexistente - vira HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
