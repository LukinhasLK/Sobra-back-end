package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha,Long> {
}
