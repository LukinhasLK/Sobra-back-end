package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao,Long> {
}
