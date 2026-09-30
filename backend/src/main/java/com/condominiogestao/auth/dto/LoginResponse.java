package com.condominiogestao.auth.dto;

import com.condominiogestao.termos.dto.TermosPendenteResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Se a pessoa só tem 1 contexto ativo, {@code token} já vem preenchido (login completo
 * numa tacada só). Se tem mais de 1, {@code token} vem nulo e {@code preAuthToken} deve
 * ser usado em {@code POST /api/auth/contexto} junto com a escolha do usuário.
 *
 * <p>{@code ultimoLoginAnterior}: valor de {@code Pessoa.ultimoLogin} de ANTES deste login
 * (null se é a primeira vez que essa pessoa loga) - o frontend guarda isso na sessão pra
 * usar como "desde quando" no alerta de mudança de status do morador, já que por essa
 * altura o backend já sobrescreveu o campo com o timestamp de agora.
 *
 * <p>{@code termosPendente}: não nulo quando a pessoa precisa aceitar o termo de
 * responsabilidade (nunca aceitou, ou aceitou uma versão anterior à vigente) - ver {@code
 * TermosService}. Calculado independente de {@code contextos}/{@code token}, então vem
 * preenchido tanto no login direto (1 vínculo) quanto no fluxo de {@code preAuthToken}
 * (a pessoa é a mesma, o termo não depende de qual contexto ela escolhe).
 */
public record LoginResponse(
        Integer pessoaId,
        String nome,
        List<ContextoDto> contextos,
        String token,
        String preAuthToken,
        LocalDateTime ultimoLoginAnterior,
        TermosPendenteResponse termosPendente) {
}
