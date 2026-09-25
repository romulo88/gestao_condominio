package com.condominiogestao.pessoa;

import com.condominiogestao.common.ResourceNotFoundException;
import com.condominiogestao.pessoa.dto.PessoaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PessoaService {

    private final PessoaRepository repository;

    public PessoaService(PessoaRepository repository) {
        this.repository = repository;
    }

    /** 404 quando o e-mail não corresponde a nenhuma pessoa cadastrada ainda. */
    public PessoaResponse buscarPorEmail(String email) {
        String normalizado = email.trim().toLowerCase();
        return repository
                .findByEmail(normalizado)
                .map(PessoaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma pessoa com o e-mail " + normalizado));
    }
}
