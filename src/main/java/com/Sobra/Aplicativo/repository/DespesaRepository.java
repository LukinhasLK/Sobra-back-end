package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Todas as consultas filtram por usuarioId (regra 2 da seção 5.12) e usam intervalo
 * semiaberto de datas [início do mês, início do mês seguinte), que aproveita ix_despesa_usuario_data.
 */
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    /** Q2a: despesas do mês em ordem crescente de data (empate por id). */
    @Query("""
            SELECT d FROM Despesa d JOIN FETCH d.categoria
            WHERE d.usuarioId = :usuarioId
              AND d.data >= :mesInicio AND d.data < :mesFim
            ORDER BY d.data, d.id
            """)
    List<Despesa> doMes(@Param("usuarioId") Long usuarioId,
                        @Param("mesInicio") LocalDate mesInicio,
                        @Param("mesFim") LocalDate mesFim);

    /** Q2b: total do mês (0.00 se não houver despesas). */
    @Query(value = """
            SELECT COALESCE(SUM(valor), 0) AS total
            FROM despesa
            WHERE usuario_id = :usuarioId
              AND data >= :mesInicio AND data < :mesFim
            """, nativeQuery = true)
    BigDecimal totalDoMes(@Param("usuarioId") Long usuarioId,
                          @Param("mesInicio") LocalDate mesInicio,
                          @Param("mesFim") LocalDate mesFim);

    /** Q1b: gasto por categoria no mês, só categorias com gasto. [codigo, nome, total] */
    @Query(value = """
            SELECT c.codigo, c.nome, SUM(d.valor) AS total
            FROM despesa d
            JOIN categoria c ON c.id = d.categoria_id
            WHERE d.usuario_id = :usuarioId
              AND d.data >= :mesInicio AND d.data < :mesFim
            GROUP BY c.id, c.codigo, c.nome
            ORDER BY total DESC, c.id
            """, nativeQuery = true)
    List<Object[]> gastoPorCategoria(@Param("usuarioId") Long usuarioId,
                                     @Param("mesInicio") LocalDate mesInicio,
                                     @Param("mesFim") LocalDate mesFim);
}
