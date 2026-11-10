package com.Sobra.Aplicativo.dominio;

import java.util.Locale;

/** RN-09: primeiro nome e iniciais, derivados do nome completo (não há coluna para eles). */
public final class Nomes {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Nomes() {
    }

    private static String[] palavras(String nomeCompleto) {
        String limpo = nomeCompleto == null ? "" : nomeCompleto.trim();
        return limpo.isEmpty() ? new String[0] : limpo.split("\\s+");
    }

    /** "Lucas Carvalho" → "Lucas"; "lucas" → "lucas". */
    public static String primeiroNome(String nomeCompleto) {
        String[] p = palavras(nomeCompleto);
        return p.length == 0 ? "" : p[0];
    }

    /** "Lucas Carvalho" → "LC"; "lucas" → "L"; "Ana Maria de Souza" → "AS". */
    public static String iniciais(String nomeCompleto) {
        String[] p = palavras(nomeCompleto);
        if (p.length == 0) {
            return "";
        }
        String primeira = p[0].substring(0, p[0].offsetByCodePoints(0, 1));
        if (p.length == 1) {
            return primeira.toUpperCase(PT_BR);
        }
        String ultimaPalavra = p[p.length - 1];
        String ultima = ultimaPalavra.substring(0, ultimaPalavra.offsetByCodePoints(0, 1));
        return (primeira + ultima).toUpperCase(PT_BR);
    }
}
