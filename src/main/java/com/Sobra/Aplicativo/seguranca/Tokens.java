package com.Sobra.Aplicativo.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * RNF-03: token = 32 bytes de gerador criptográfico, entregue uma única vez.
 * No banco fica só o SHA-256 em hexadecimal (64 caracteres).
 */
public final class Tokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Tokens() {
    }

    /** 32 bytes aleatórios em Base64 URL-safe sem "=" (43 caracteres). */
    public static String gerar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 em hexadecimal minúsculo. */
    public static String hash(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
