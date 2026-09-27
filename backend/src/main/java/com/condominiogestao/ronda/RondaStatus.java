package com.condominiogestao.ronda;

public enum RondaStatus {
    em_andamento,
    /** O próprio rondista tocou "Finalizar ronda". */
    finalizada,
    /** Um perfil completo (síndico/sub-síndico/encarregado/supervisor) encerrou pela tela
     * "Rondas" uma ronda de outro rondista que ficou esquecida em andamento. */
    encerrada_manualmente,
    /** Ninguém fechou (nem o rondista, nem um gestor) e já se passaram 12h desde o início -
     * aplicado sob demanda, na primeira leitura que tocar essa ronda (ver
     * {@code RondaService.fecharSeAbandonada}), sem job agendado. */
    encerrada_automaticamente
}
