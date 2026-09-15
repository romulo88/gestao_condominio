package com.condominiogestao.common;

/** Lançada quando uma regra de unicidade/negócio é violada (ex: CNPJ já cadastrado) - vira HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
