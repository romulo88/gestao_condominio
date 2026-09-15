package com.condominiogestao.common;

/**
 * Lançada quando os dados da requisição são individualmente válidos, mas inconsistentes
 * entre si (ex: bloco informado pertence a outro condomínio) - vira HTTP 400, igual erro
 * de validação de campo.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
