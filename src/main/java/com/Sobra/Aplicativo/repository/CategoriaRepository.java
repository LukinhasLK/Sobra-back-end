package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Short> {

    /** As 7 categorias na ordem das telas (id 1 a 7). */
    List<Categoria> findAllByOrderByIdAsc();

    Optional<Categoria> findByCodigo(String codigo);
}
