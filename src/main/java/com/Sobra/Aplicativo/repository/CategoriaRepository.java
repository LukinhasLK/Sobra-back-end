package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Categorias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categorias,Long> {
}
