package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Objetivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjetivoRepository extends JpaRepository<Objetivo,Long> {
}
