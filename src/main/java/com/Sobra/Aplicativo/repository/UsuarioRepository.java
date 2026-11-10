package com.Sobra.Aplicativo.repository;

import com.Sobra.Aplicativo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** O e-mail já chega normalizado (minúsculo), igual ao CHECK do banco. */
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
