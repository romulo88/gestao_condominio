package com.condominiogestao.common;

/**
 * Pessoa autenticada, mas sem permissão pra essa ação (ex: morador tentando criar um
 * aviso) - vira HTTP 403. Diferente de {@link UnauthorizedException} (401), que é sobre
 * não estar autenticado / token inválido.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
