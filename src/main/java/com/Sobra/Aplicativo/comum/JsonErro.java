package com.Sobra.Aplicativo.comum;

import java.nio.charset.StandardCharsets;

/**
 * Monta o corpo do formato único de erro (seção 6.2) à mão.
 * É usado tanto pelos filtros (antes do Spring MVC) quanto pelo TratadorErros,
 * garantindo exatamente o mesmo formato em todas as respostas de erro.
 */
public final class JsonErro {

    private JsonErro() {
    }

    public static byte[] corpo(ErroApi erro, String idRequisicao) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"erro\":{");
        sb.append("\"codigo\":").append(texto(erro.getCodigo())).append(',');
        sb.append("\"mensagem\":").append(texto(erro.getMessage())).append(',');
        sb.append("\"campos\":[");
        boolean primeiro = true;
        for (CampoErro c : erro.getCampos()) {
            if (!primeiro) {
                sb.append(',');
            }
            primeiro = false;
            sb.append("{\"campo\":").append(texto(c.campo()))
              .append(",\"codigo\":").append(texto(c.codigo()))
              .append(",\"mensagem\":").append(texto(c.mensagem())).append('}');
        }
        sb.append("],");
        sb.append("\"id_requisicao\":").append(texto(idRequisicao));
        sb.append("}}");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** String JSON com escape. */
    public static String texto(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
