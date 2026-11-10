package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {

    Optional<RecuperacaoSenha> findByTokenHash(String tokenHash);

    /** Invalida pedidos anteriores ainda não usados quando chega um novo (ix_recuperacao_senha_usuario). */
    @Modifying
    @Query("""
            UPDATE RecuperacaoSenha r SET r.usadoEm = :agora
            WHERE r.usuarioId = :usuarioId AND r.usadoEm IS NULL AND r.criadoEm <= :agora
            """)
    int invalidarPendentes(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);
}
