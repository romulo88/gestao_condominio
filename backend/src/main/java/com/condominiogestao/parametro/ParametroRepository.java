package com.condominiogestao.parametro;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroRepository extends JpaRepository<Parametro, Integer> {

    Optional<Parametro> findByNome(String nome);

    boolean existsByNome(String nome);
}
