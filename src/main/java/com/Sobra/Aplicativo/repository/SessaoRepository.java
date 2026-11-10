package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    /** Q8: busca pelo hash do token (índice único uq_sessao_token). */
    Optional<Sessao> findByTokenHash(String tokenHash);

    /** Revoga todas as sessões ainda abertas da pessoa (troca de senha). Usa ix_sessao_usuario. */
    @Modifying
    @Query("""
            UPDATE Sessao s SET s.revogadaEm = :agora
            WHERE s.usuarioId = :usuarioId AND s.revogadaEm IS NULL AND s.criadoEm <= :agora
            """)
    int revogarTodas(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);
}
