package com.condominiogestao.morador;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoradorRepository extends JpaRepository<Morador, Integer> {

    Optional<Morador> findByPessoaEmail(String email);
}
