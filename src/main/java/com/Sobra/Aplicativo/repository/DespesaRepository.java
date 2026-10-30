package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Despesas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DespesaRepository extends JpaRepository<Despesas,Long> {
}
