package com.Sobra.Aplicativo.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Regras de objetivo da seção 7: RN-03 (progresso), RN-04 (aporte sugerido) e RN-08 (previsão).
 * Classe sem dependências para ser testada isoladamente.
 */
public final class RegrasObjetivo {

    private RegrasObjetivo() {
    }

    /**
     * RN-04 / D-05: aporte mensal = valor desejado ÷ prazo, 2 casas, HALF_UP.
     * 15000 ÷ 18 = 833.33; 1 ÷ 8 = 0.13.
     */
    public static BigDecimal aporteSugerido(BigDecimal valorMeta, int prazoMeses) {
        return valorMeta.divide(BigDecimal.valueOf(prazoMeses), 2, RoundingMode.HALF_UP);
    }

    /**
     * RN-03: acumulado ÷ meta em %, 2 casas, limitado a 100.00.
     * Mesma conta das consultas Q3/Q4a (o banco também a faz; esta versão serve para a resposta do aporte).
     */
    public static BigDecimal percentual(BigDecimal acumulado, BigDecimal valorMeta) {
        if (acumulado.compareTo(valorMeta) >= 0) {
            return new BigDecimal("100.00");
        }
        if (acumulado.signum() <= 0) {
            return Dinheiro.ZERO;
        }
        return Dinheiro.CEM.multiply(acumulado).divide(valorMeta, 2, RoundingMode.HALF_UP);
    }

    /** Concluído quando o que falta é zero ou negativo. */
    public static boolean concluido(BigDecimal acumulado, BigDecimal valorMeta) {
        return valorMeta.subtract(acumulado).signum() <= 0;
    }

    /**
     * RN-08 / D-04: previsão = mês de referência + N meses,
     * N = arredondar para cima( ROUND(restante ÷ aporte mensal, 2) ).
     * Devolve null quando o objetivo já está concluído.
     */
    public static YearMonth previsaoConclusao(BigDecimal valorMeta, BigDecimal acumulado,
                                              BigDecimal aporteMensal, YearMonth referencia) {
        BigDecimal restante = valorMeta.subtract(acumulado);
        if (restante.signum() <= 0) {
            return null;
        }
        BigDecimal razao = restante.divide(aporteMensal, 2, RoundingMode.HALF_UP);
        int meses = razao.setScale(0, RoundingMode.CEILING).intValueExact();
        return referencia.plusMonths(meses);
    }
}
