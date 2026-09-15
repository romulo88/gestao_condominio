package com.condominiogestao.auth.dto;

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
 */
public record LoginResponse(
        Integer pessoaId,
        String nome,
        List<ContextoDto> contextos,
        String token,
        String preAuthToken,
        LocalDateTime ultimoLoginAnterior) {
}
