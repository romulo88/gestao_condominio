package com.condominiogestao.common;

import com.condominiogestao.funcionario.FuncionarioPerfil;
import com.condominiogestao.security.ContextoAutenticado;

/**
 * Checagens de autorização por perfil compartilhadas entre services. "Administrador"
 * (papel global) sempre pode; "síndico"/"sub-síndico" só podem nas ações do PRÓPRIO
 * condomínio - comparado sempre com o {@code condominioId} do contexto/token (quem
 * logou), nunca com um valor vindo do corpo da requisição, pra ninguém conseguir
 * gerenciar um condomínio que não é o seu só mudando o id na chamada.
 */
public final class Autorizacao {

    private Autorizacao() {}

    public static boolean ehAdministrador(ContextoAutenticado contexto) {
        return TipoPessoa.administrador.name().equals(contexto.tipoPapel());
    }

    public static boolean ehGestorDoCondominio(ContextoAutenticado contexto, Integer condominioId) {
        return TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && (FuncionarioPerfil.sindico.name().equals(contexto.perfil())
                        || FuncionarioPerfil.sub_sindico.name().equals(contexto.perfil()))
                && condominioId != null
                && condominioId.equals(contexto.condominioId());
    }

    /** Qualquer funcionário (qualquer perfil, inclusive sem perfil não faria login) desse
     * condomínio - mais amplo que {@link #ehGestorDoCondominio}, que é só síndico/sub-síndico. */
    public static boolean ehFuncionarioDoCondominio(ContextoAutenticado contexto, Integer condominioId) {
        return TipoPessoa.funcionario.name().equals(contexto.tipoPapel())
                && condominioId != null
                && condominioId.equals(contexto.condominioId());
    }

    /** Ação restrita a funcionário (qualquer perfil) do próprio condomínio - nem
     * administrador, nem morador, nem funcionário de outro condomínio. */
    public static void exigirFuncionarioDoCondominio(ContextoAutenticado contexto, Integer condominioId) {
        if (!ehFuncionarioDoCondominio(contexto, condominioId)) {
            throw new ForbiddenException("Só funcionário deste condomínio pode fazer isso");
        }
    }

    /** Mesma ideia de {@link #ehFuncionarioDoCondominio}, mas pra morador. */
    public static boolean ehMoradorDoCondominio(ContextoAutenticado contexto, Integer condominioId) {
        return TipoPessoa.morador.name().equals(contexto.tipoPapel())
                && condominioId != null
                && condominioId.equals(contexto.condominioId());
    }

    /** Ver algo de um condomínio específico incluindo morador (leitura ampla - ex: listar
     * etiquetas pro filtro do Kanban) - mais permissivo que
     * {@link #exigirAdministradorOuFuncionarioDoCondominio}, que não deixa morador passar. */
    public static void exigirAdministradorOuFuncionarioOuMoradorDoCondominio(
            ContextoAutenticado contexto, Integer condominioId) {
        if (!ehAdministrador(contexto)
                && !ehFuncionarioDoCondominio(contexto, condominioId)
                && !ehMoradorDoCondominio(contexto, condominioId)) {
            throw new ForbiddenException("Só administrador, funcionário ou morador deste condomínio pode fazer isso");
        }
    }

    /** Editar/gerenciar um condomínio específico: administrador, ou síndico/sub-síndico dele mesmo. */
    public static void exigirAdministradorOuGestor(ContextoAutenticado contexto, Integer condominioId) {
        if (!ehAdministrador(contexto) && !ehGestorDoCondominio(contexto, condominioId)) {
            throw new ForbiddenException("Só administrador, síndico ou sub-síndico deste condomínio pode fazer isso");
        }
    }

    /** Ver/moderar algo de um condomínio específico: administrador (qualquer condomínio,
     * função de supervisão) OU qualquer funcionário desse condomínio (qualquer perfil). */
    public static void exigirAdministradorOuFuncionarioDoCondominio(ContextoAutenticado contexto, Integer condominioId) {
        if (!ehAdministrador(contexto) && !ehFuncionarioDoCondominio(contexto, condominioId)) {
            throw new ForbiddenException("Só administrador ou funcionário deste condomínio pode fazer isso");
        }
    }

    /** Ações restritas ao administrador global (ex: criar/desativar condomínio, criar outro administrador). */
    public static void exigirAdministrador(ContextoAutenticado contexto) {
        if (!ehAdministrador(contexto)) {
            throw new ForbiddenException("Só administrador pode fazer isso");
        }
    }
}
