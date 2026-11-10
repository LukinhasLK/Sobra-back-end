package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Aporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Aporte nunca é consultado sozinho: sempre ligado ao objetivo do usuário (P-06, regra 3). */
public interface AporteRepository extends JpaRepository<Aporte, Long> {

    /** Q4b: histórico do mais recente para o mais antigo. */
    @Query("""
            SELECT a FROM Aporte a, Objetivo o
            WHERE o.id = a.objetivoId
              AND o.id = :objetivoId AND o.usuarioId = :usuarioId
            ORDER BY a.data DESC, a.id DESC
            """)
    List<Aporte> historico(@Param("objetivoId") Long objetivoId, @Param("usuarioId") Long usuarioId);
}
