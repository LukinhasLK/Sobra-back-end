package com.Sobra.Aplicativo.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor de JSON mínimo para os testes (Map, List, String, BigDecimal, Boolean, null).
 * Não depende da versão do Jackson, então os testes leem a resposta exatamente como o app leria.
 */
public final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    public static Object ler(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        Json j = new Json(texto);
        Object v = j.valor();
        j.espacos();
        if (j.i != j.s.length()) {
            throw new IllegalArgumentException("Sobrou texto após o JSON na posição " + j.i);
        }
        return v;
    }

    private Object valor() {
        espacos();
        char c = s.charAt(i);
        switch (c) {
            case '{': return objeto();
            case '[': return lista();
            case '"': return texto();
            case 't': esperar("true"); return Boolean.TRUE;
            case 'f': esperar("false"); return Boolean.FALSE;
            case 'n': esperar("null"); return null;
            default: return numero();
        }
    }

    private Map<String, Object> objeto() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        espacos();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            espacos();
            String chave = texto();
            espacos();
            if (s.charAt(i++) != ':') throw new IllegalArgumentException("Esperado ':' em " + i);
            m.put(chave, valor());
            espacos();
            char c = s.charAt(i++);
            if (c == '}') return m;
            if (c != ',') throw new IllegalArgumentException("Esperado ',' ou '}' em " + i);
        }
    }

    private List<Object> lista() {
        List<Object> l = new ArrayList<>();
        i++;
        espacos();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            l.add(valor());
            espacos();
            char c = s.charAt(i++);
            if (c == ']') return l;
            if (c != ',') throw new IllegalArgumentException("Esperado ',' ou ']' em " + i);
        }
    }

    private String texto() {
        if (s.charAt(i++) != '"') throw new IllegalArgumentException("Esperado '\"' em " + i);
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private BigDecimal numero() {
        int ini = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        return new BigDecimal(s.substring(ini, i));
    }

    private void esperar(String palavra) {
        if (!s.startsWith(palavra, i)) throw new IllegalArgumentException("Esperado " + palavra + " em " + i);
        i += palavra.length();
    }

    private void espacos() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    // ---------- navegação: Json.em(corpo, "erro.campos.0.codigo")

    @SuppressWarnings("unchecked")
    public static Object em(Object raiz, String caminho) {
        Object atual = raiz;
        for (String parte : caminho.split("\\.")) {
            if (atual instanceof Map<?, ?> m) {
                if (!m.containsKey(parte)) throw new AssertionError("Campo ausente: " + caminho + " (parte " + parte + ")");
                atual = ((Map<String, Object>) m).get(parte);
            } else if (atual instanceof List<?> l) {
                atual = l.get(Integer.parseInt(parte));
            } else {
                throw new AssertionError("Caminho inválido: " + caminho);
            }
        }
        return atual;
    }
}
