package com.condominiogestao.funcionario.dto;

import com.condominiogestao.funcionario.FuncionarioPerfil;
import jakarta.validation.constraints.Email;

/** Corrige o perfil de um vínculo já existente e o e-mail/telefone da pessoa por trás dele
 * - trocar de funcionário ou de condomínio não é "editar", é um vínculo novo (ver
 * {@code FuncionarioCondominioCreateRequest}). Nome não entra aqui de propósito -
 * identidade não se edita por essa tela. {@code perfil} nulo = funcionário sem acesso ao
 * sistema nesse condomínio (mesmo significado do cadastro, item 2.1). E-mail só é exigido
 * de verdade quando {@code perfil} não é nulo - ver
 * {@code FuncionarioCondominioService.atualizar} (checagem em tempo de execução, não dá
 * pra expressar "obrigatório condicional a outro campo" só com anotação de bean validation).
 * {@code telefone} vem sem máscara (LGPD, v177) - vazio/nulo apaga o telefone existente,
 * diferente do e-mail (que nunca é apagado, só trocado). */
public record FuncionarioCondominioUpdateRequest(
        FuncionarioPerfil perfil,
        @Email(message = "email inválido") String email,
        String telefone,
        String funcao) {
}
