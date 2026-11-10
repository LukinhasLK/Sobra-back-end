package com.Sobra.Aplicativo.seguranca;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * RNF-02: Argon2id com os parâmetros mínimos da OWASP
 * (memória 19 MiB = 19456 KiB, 2 iterações, paralelismo 1). O texto gerado já inclui sal e parâmetros:
 * $argon2id$v=19$m=19456,t=2,p=1$...
 */
@Component
public class SenhaHasher {

    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

    /** Hash de uma senha qualquer, usado para gastar o mesmo tempo quando o e-mail não existe (RF-02). */
    private final String hashFicticio = encoder.encode("senha-ficticia-para-igualar-tempo");

    public String gerar(String senha) {
        return encoder.encode(senha);
    }

    public boolean confere(String senha, String hash) {
        try {
            return hash != null && encoder.matches(senha, hash);
        } catch (RuntimeException e) {
            return false; // hash inválido (ex.: dados de exemplo) nunca autentica
        }
    }

    /** Faz a mesma conta de uma verificação real, sem resultado útil. */
    public void gastarTempoEquivalente(String senha) {
        encoder.matches(senha == null ? "" : senha, hashFicticio);
    }
}
