package com.condominiogestao.pessoa;

import com.condominiogestao.common.Cpf;
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

    /** 404 quando o CPF não corresponde a nenhuma pessoa cadastrada ainda. */
    public PessoaResponse buscarPorCpf(String cpf) {
        String normalizado = Cpf.normalizar(cpf);
        return repository
                .findByCpf(normalizado)
                .map(PessoaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma pessoa com o CPF " + normalizado));
    }
}
