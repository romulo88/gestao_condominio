package com.condominiogestao.funcionario;

public enum FuncionarioPerfil {
    sindico,
    sub_sindico,
    supervisor,
    encarregado,
    /** Perfis de acesso restrito (pedido do Romulo) - rondista de ronda noturna: só
     * cadastra demanda e acompanha as próprias/onde é responsável, sem poder aprovar,
     * reprovar, mover no Kanban, nem ver o quadro Kanban (ver {@link #acessoRestrito}). */
    rondista,
    /** Mesmo espírito de {@link #rondista} - agente de convívio das áreas comuns. */
    agente_convivio;

    /** Perfil de acesso restrito - só cadastra demanda e acompanha as próprias/onde é
     * responsável (ver `DemandaService.podeVer`/`exigirFuncionarioDoCondominio`). Recebe
     * `String` de propósito - é o tipo que `ContextoAutenticado.perfil()` já expõe, evita
     * parse/try-catch de enum em todo chamador. */
    public static boolean acessoRestrito(String perfil) {
        return perfil != null && (perfil.equals(rondista.name()) || perfil.equals(agente_convivio.name()));
    }
}
