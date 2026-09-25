package com.condominiogestao.common;

/**
 * CNPJ é guardado e comparado sempre sem pontuação (só os 14 dígitos) - evita que
 * "53.409.716/0001-03" e "53409716000103" sejam tratados como CNPJs diferentes por
 * {@code existsByCnpj}.
 */
public final class Cnpj {

    private Cnpj() {}

    public static String normalizar(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "");
    }
}
