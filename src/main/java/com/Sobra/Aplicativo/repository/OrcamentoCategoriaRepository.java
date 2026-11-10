package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.OrcamentoCategoria;
import com.Sobra.Aplicativo.model.OrcamentoCategoriaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrcamentoCategoriaRepository extends JpaRepository<OrcamentoCategoria, OrcamentoCategoriaId> {

    /** Etapa 2 da configuração: quantas das 7 categorias a pessoa já estimou. */
    @Query("SELECT COUNT(o.categoriaId) FROM OrcamentoCategoria o WHERE o.usuarioId = :usuarioId")
    long contarDoUsuario(@Param("usuarioId") Long usuarioId);

    /** Q7: as 7 categorias com a estimativa da pessoa (nulo = inativa ou não informada). [codigo, nome, valor_estimado] */
    @Query(value = """
            SELECT c.codigo, c.nome, oc.valor_estimado
            FROM categoria c
            LEFT JOIN orcamento_categoria oc
                   ON oc.categoria_id = c.id AND oc.usuario_id = :usuarioId
            ORDER BY c.id
            """, nativeQuery = true)
    List<Object[]> estimativas(@Param("usuarioId") Long usuarioId);
}
