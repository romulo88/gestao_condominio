package com.condominiogestao.common;

public enum TipoPessoa {
    funcionario,
    morador,
    /** Papel global, sem condomínio associado - ver {@code administradores} na migration. */
    administrador
}
