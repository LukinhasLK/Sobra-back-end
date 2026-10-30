package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.RendaVigencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RendaVirgenciaRepository extends JpaRepository<RendaVigencia,Long> {
}
