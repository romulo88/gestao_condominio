package com.condominiogestao.common;

/**
 * CPF é guardado e comparado sempre sem pontuação (só os 11 dígitos) - normalizar aqui
 * em todo ponto de entrada (login, cadastro) evita que um CPF digitado com máscara
 * (123.456.789-00) deixe de bater com um já salvo sem ela, ou vice-versa.
 */
public final class Cpf {

    private Cpf() {}

    public static String normalizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}
