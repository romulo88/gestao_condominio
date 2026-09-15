package com.condominiogestao.pessoa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {

    Optional<Pessoa> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
}
