package com.condominiogestao.common;

import java.time.LocalDateTime;
import java.util.List;

/** Corpo padrão de erro devolvido pela API (ver {@link GlobalExceptionHandler}). */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> detalhes) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message, List<String> detalhes) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, detalhes);
    }
}
