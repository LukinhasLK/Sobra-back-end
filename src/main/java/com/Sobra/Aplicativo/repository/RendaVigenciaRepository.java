package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.RendaVigencia;
import com.Sobra.Aplicativo.model.RendaVigenciaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RendaVigenciaRepository extends JpaRepository<RendaVigencia, RendaVigenciaId> {

    /** Q1a: renda vigente no mês = linha com o maior mes_inicio <= mês pedido. Nulo se não houver. */
    @Query(value = """
            SELECT r.valor AS renda
            FROM renda_vigencia r
            WHERE r.usuario_id = :usuarioId
              AND r.mes_inicio = (SELECT MAX(r2.mes_inicio)
                                  FROM renda_vigencia r2
                                  WHERE r2.usuario_id = r.usuario_id
                                    AND r2.mes_inicio <= :mesInicio)
            """, nativeQuery = true)
    BigDecimal rendaVigente(@Param("usuarioId") Long usuarioId, @Param("mesInicio") LocalDate mesInicio);

    /** Etapa 1 da configuração: a pessoa já informou alguma renda? */
    @Query("SELECT COUNT(r.mesInicio) FROM RendaVigencia r WHERE r.usuarioId = :usuarioId")
    long contarDoUsuario(@Param("usuarioId") Long usuarioId);
}
