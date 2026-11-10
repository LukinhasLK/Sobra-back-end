package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Simulacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SimulacaoRepository extends JpaRepository<Simulacao, Long> {

    /** Q5: da mais recente para a mais antiga; o Pageable aplica o "limite". */
    @Query("""
            SELECT s FROM Simulacao s
            WHERE s.usuarioId = :usuarioId
            ORDER BY s.criadoEm DESC, s.id DESC
            """)
    List<Simulacao> doUsuario(@Param("usuarioId") Long usuarioId, Pageable pagina);
}
