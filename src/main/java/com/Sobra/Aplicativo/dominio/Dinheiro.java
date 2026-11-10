package com.Sobra.Aplicativo.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * RN-01: dinheiro é sempre BigDecimal com duas casas, nunca double.
 * No JSON vai como string ("1200.00").
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal CEM = new BigDecimal("100");

    private Dinheiro() {
    }

    /** Arredonda para 2 casas, meia unidade para cima (HALF_UP). */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Formata para o contrato: "1200.00". Nulo continua nulo. */
    public static String texto(BigDecimal valor) {
        return valor == null ? null : arredondar(valor).toPlainString();
    }

    /** Taxa com 6 casas: "0.009600". */
    public static String taxa(BigDecimal taxa) {
        return taxa == null ? null : taxa.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    /** Converte o que vem de uma consulta nativa (BigDecimal, Long, Integer...) em BigDecimal. */
    public static BigDecimal de(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(valor.toString());
    }
}
