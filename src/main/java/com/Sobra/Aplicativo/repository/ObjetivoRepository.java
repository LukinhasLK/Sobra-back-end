package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Objetivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Acumulado e percentual saem sempre da mesma consulta (RN-03): nunca divergem. */
public interface ObjetivoRepository extends JpaRepository<Objetivo, Long> {

    Optional<Objetivo> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Q3: lista com acumulado e percentual. [id, nome, valor_meta, imagem_url, valor_acumulado, percentual_concluido] */
    @Query(value = """
            SELECT o.id, o.nome, o.valor_meta, o.imagem_url,
                   COALESCE(SUM(a.valor), 0) AS valor_acumulado,
                   CASE WHEN COALESCE(SUM(a.valor), 0) >= o.valor_meta THEN 100.00
                        ELSE ROUND(100 * COALESCE(SUM(a.valor), 0) / o.valor_meta, 2)
                   END AS percentual_concluido
            FROM objetivo o
            LEFT JOIN aporte a ON a.objetivo_id = o.id
            WHERE o.usuario_id = :usuarioId
            GROUP BY o.id, o.nome, o.valor_meta, o.imagem_url, o.criado_em
            ORDER BY o.criado_em, o.id
            """, nativeQuery = true)
    List<Object[]> listarComAcumulado(@Param("usuarioId") Long usuarioId);

    /**
     * Q4a: cabeçalho do detalhe. Lista vazia = inexistente OU de outro usuário (ambos viram 404).
     * [id, nome, valor_meta, prazo_meses, aporte_mensal, imagem_url, valor_acumulado, percentual_concluido]
     */
    @Query(value = """
            SELECT o.id, o.nome, o.valor_meta, o.prazo_meses, o.aporte_mensal, o.imagem_url,
                   COALESCE(SUM(a.valor), 0) AS valor_acumulado,
                   CASE WHEN COALESCE(SUM(a.valor), 0) >= o.valor_meta THEN 100.00
                        ELSE ROUND(100 * COALESCE(SUM(a.valor), 0) / o.valor_meta, 2)
                   END AS percentual_concluido
            FROM objetivo o
            LEFT JOIN aporte a ON a.objetivo_id = o.id
            WHERE o.id = :objetivoId AND o.usuario_id = :usuarioId
            GROUP BY o.id, o.nome, o.valor_meta, o.prazo_meses, o.aporte_mensal, o.imagem_url
            """, nativeQuery = true)
    List<Object[]> detalhe(@Param("objetivoId") Long objetivoId, @Param("usuarioId") Long usuarioId);
}
