package com.condominiogestao.common;

/**
 * Constantes em minúsculo (fora da convenção Java) de propósito: com
 * {@code @Enumerated(EnumType.STRING)}, o Hibernate grava {@code Enum.name()} tal
 * qual no banco - manter em minúsculo evita um conversor a mais só pra bater com
 * os valores já usados nas colunas VARCHAR (ver V1__init.sql).
 */
public enum Situacao {
    ativo,
    inativo
}
