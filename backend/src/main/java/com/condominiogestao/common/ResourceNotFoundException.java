package com.condominiogestao.common;

/** Lançada quando um recurso buscado por id não existe - vira HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
