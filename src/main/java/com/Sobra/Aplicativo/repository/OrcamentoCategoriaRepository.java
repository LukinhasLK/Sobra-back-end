package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.OrcamentoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrcamentoCategoriaRepository extends JpaRepository<OrcamentoCategoria,Long> {
}
