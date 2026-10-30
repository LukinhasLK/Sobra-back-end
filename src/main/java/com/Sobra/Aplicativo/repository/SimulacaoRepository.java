package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Simulacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulacaoRepository extends JpaRepository<Simulacao,Long> {
}
