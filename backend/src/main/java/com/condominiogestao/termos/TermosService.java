package com.condominiogestao.termos;

import com.condominiogestao.common.UnauthorizedException;
import com.condominiogestao.pessoa.Pessoa;
import com.condominiogestao.pessoa.PessoaRepository;
import com.condominiogestao.security.ContextoAutenticado;
import com.condominiogestao.termos.dto.TermosPendenteResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Termo de responsabilidade (pedido do Romulo) - ver {@link TermosResponsabilidade}. */
@Service
public class TermosService {

    private final PessoaRepository pessoaRepository;

    public TermosService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    /** {@code null} quando a pessoa já aceitou a versão vigente - usado por {@code
     * AuthService#login} pra preencher {@code LoginResponse.termosPendente}. */
    public TermosPendenteResponse calcularPendente(Pessoa pessoa) {
        Integer versaoAceita = pessoa.getTermosVersaoAceita();
        if (versaoAceita != null && versaoAceita >= TermosResponsabilidade.VERSAO_ATUAL) {
            return null;
        }
        return new TermosPendenteResponse(TermosResponsabilidade.VERSAO_ATUAL, TermosResponsabilidade.TEXTO);
    }

    /** Grava a versão vigente como aceita por quem está logado - chamado só depois que a
     * pessoa já viu o texto e confirmou ("Li e concordo"). */
    @Transactional
    public void aceitar(ContextoAutenticado contexto) {
        Pessoa pessoa = pessoaRepository
                .findById(contexto.pessoaId())
                .orElseThrow(() -> new UnauthorizedException("Pessoa não existe mais"));
        pessoa.setTermosVersaoAceita(TermosResponsabilidade.VERSAO_ATUAL);
        pessoa.setTermosAceitosEm(LocalDateTime.now());
        pessoaRepository.save(pessoa);
    }
}
