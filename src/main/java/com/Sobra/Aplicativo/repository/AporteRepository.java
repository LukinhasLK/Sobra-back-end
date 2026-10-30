package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Aporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AporteRepository extends JpaRepository<Aporte,Long> {
}
